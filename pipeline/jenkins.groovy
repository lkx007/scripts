def config = new LinkedHashMap([
        "develop"  : [
                "project" : "",
                "services": [
                        "csp"     : "18080",
                        "cmp"     : "18081",
                        "cmdb"    : "18082",
                        "soc"     : "18088",
                        "cop"     : "18090",
                        "cmc"     : "18091",
                        "csc"     : "18092",
                        "exporter": "18094",
                        "css"     : "18095",
                        "cos"     : "18096",
                ],
                base      : ["entity", "worker", "pontus"]
        ],
        "edge"     : [
                "project" : "project/edge/",
                "services": [
                        "csp"     : "18080",
                        "cmp"     : "18081",
                        "soc"     : "18088",
                        "cmc"     : "18091",
                        "csc"     : "18092",
                        "exporter": "18094",
                        "css"     : "18095",
                        "cos"     : "18096",
                ],
                base      : ["entity"]
        ],
        "fj_mobile": [
                "project" : "project/fj_mobile/",
                "services": [
                        "csp"     : "18080",
                        "cmp"     : "18081",
                        "cmdb"    : "18082",
                        "soc"     : "18088",
                        "cop"     : "18090",
                        "cmc"     : "18091",
                        "csc"     : "18092",
                        "exporter": "18094",
                        "css"     : "18095",
                        "cos"     : "18096",
                ],
                base      : ["entity", "worker"]
        ]
])

node() {
    config.each { entry ->
        if (entry.key.equals(params.project)) {
            def params = new LinkedHashMap<>(entry.value)
            def version
            params.get("base").each { base ->
                // base里面必须不能把worker和pontus放在第一个
                if (base.equals("worker")) {
                    buildWorker(params.get("project"), version, "develop")
                } else if (base.equals("pontus")) {
                    buildPontus(params.get("project"), version, "develop")
                } else {
                    println(String.format("start to build project=%s service=%s, branch=",
                            params.get("project"), base, "develop"))
                    version = buildService(params.get("project"), base, "", "develop")
                }
            }
            buildWeb(params.get("project"), version, "develop")
            params.get("services").keySet().each { service ->
                // css在cmp中打包
                if (!service.equals("css")) {
                    println(String.format("start to build project=%s service=%s, port=%s, branch=",
                            params.get("project"), service, params.get("services").get(service), "develop"))
                    buildService(params.get("project"), service, params.get("services").get(service), "develop")
                }
            }
        }
    }
}

def buildService(project, service, port, branch) {
    stage(String.format('prepare %s', service)) {
        sh "rm -rf ${WORKSPACE}/*"
    }
    if (service.equals("exporter")) {
        gitUrl = String.format('http://dingmw@gitlab:8888/BeyondCMP/%sbocloud.cmp.%s.git', project, service)
    } else {
        gitUrl = String.format('http://dingmw@gitlab:8888/BeyondCMP/%sbocloud.%s.git', project, service)
    }
    stage(String.format('clone %s code', service)) {
        //check CODE
        echo "clone ${service} code, project=${project}, branch=${branch}"
        checkout([$class                           : 'GitSCM', branches: [[name: String.format('*/%s', branch)]],
                  doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [],
                  userRemoteConfigs                : [[url: gitUrl]]
        ])
    }
    def v = version()
    if (v) {
        echo "Building version ${v}"
    }

    //定义mvn环境
    def mvnHome = tool 'maven-3.5'
    env.PATH = "${mvnHome}/bin:${env.PATH}"

    stage(String.format('test %s', service)) {
        // mvn 测试
        // sh "mvn test"
        sh "echo $project $service $port $v $branch"
    }

    stage(String.format('build %s', service)) {
        //mvn构建
        sh "mvn clean install -Dmaven.test.skip=true"
    }

    stage(String.format('deploy %s', service)) {
        // 执行部署脚本
        // mvn构建
        // sh "mvn deploy"
        sh "echo skip deploy"
    }

    stage(String.format('build %s docker image', service)) {
        if (!port.isEmpty()) {
            sh "/usr/bin/cp ~/docker/services/Dockerfile ~/docker/services/docker-entrypoint.sh ${WORKSPACE}"
            sh "/usr/bin/cp ~/docker/services/config/${service}.properties ${WORKSPACE}"
            if (service.equals("cmc") || service.equals("csc") || service.equals("exporter")) {
                sh "/usr/bin/cp ${WORKSPACE}/target/*.jar ${WORKSPACE}"
            } else if (service.equals("cmp")) {
                // build css
                sh "/usr/bin/cp ~/docker/services/config/css.properties ${WORKSPACE}"
                sh "/usr/bin/cp ${WORKSPACE}/bocloud.cmp.crawler/target/*.jar ${WORKSPACE}/bocloud.css.booter-${v}.jar"
                def dockerfile = 'Dockerfile'
                def customImage = docker.build("registry.bocloud.com.cn/cmp/bocloud.css:${v}",
                        "-f ${dockerfile} . --build-arg service=css --build-arg port=18095 --build-arg version=${v}")
                customImage.push()

                sh "/usr/bin/cp ${WORKSPACE}/bocloud.${service}.booter/target/*.jar ${WORKSPACE}"
            }else {
                sh "/usr/bin/cp ${WORKSPACE}/bocloud.${service}.booter/target/*.jar ${WORKSPACE}"
            }
            def dockerfile = 'Dockerfile'
            def customImage = docker.build("registry.bocloud.com.cn/cmp/bocloud.${service}:${v}",
                    "-f ${dockerfile} . --build-arg service=${service} --build-arg port=${port} --build-arg version=${v}")
            customImage.push()
        }
    }

    stage(String.format('clean %s', service)) {
        sh "rm -rf ${WORKSPACE}/*"
    }
    return v
}

def buildWorker(project, v, branch) {
    def service = 'worker'
    def port = '18089'
    stage(String.format('prepare %s', service)) {
        sh "rm -rf ${WORKSPACE}/*"
    }

    stage(String.format('clone %s code', service)) {
        //check CODE
        echo "clone ${service} code, project=${project}, branch=${branch}"
        checkout([$class                           : 'GitSCM', branches: [[name: String.format('*/%s', branch)]],
                  doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [],
                  userRemoteConfigs                : [[url: String.format('http://dingmw@gitlab:8888/BeyondCMP/%sbocloud.%s.git', project, service)]]])
    }

    stage(String.format('build %s', service)) {
        // rpm-build构建
        sh "cd $WORKSPACE/build;\n" +
                "\n" +
                "sed -i \"s/Release:.*/Release:                %(git rev-parse --short HEAD).%(date +%Y%m%d%H).$BUILD_ID/g\" bocloud_worker.spec\n" +
                "\n" +
                "bash build.sh release &>/dev/null\n" +
                "\n" +
                "git checkout bocloud_worker.spec\n" +
                "\n" +
                "ls dist/bocloud_worker-*.rpm"
    }

    stage(String.format('build %s docker image', service)) {
        if (!port.isEmpty()) {
            sh "/usr/bin/cp ~/docker/worker/Dockerfile ~/docker/worker/docker-entrypoint.sh ${WORKSPACE}"
            sh "/usr/bin/cp ~/docker/worker/bocloud_worker_config.yml ${WORKSPACE}"
            sh "/usr/bin/cp ${WORKSPACE}/build/dist/bocloud_worker-*.rpm ${WORKSPACE}"
            def dockerfile = 'Dockerfile'
            def customImage = docker.build("registry.bocloud.com.cn/cmp/bocloud.${service}:${v}",
                    "-f ${dockerfile} . --build-arg port=${port}")
            customImage.push()
        }
    }

    stage(String.format('clean %s', service)) {
        sh "rm -rf ${WORKSPACE}/*"
    }
}

def buildPontus(project, v, branch) {
    def service = 'pontus'
    def port = '18097'

    stage(String.format('prepare %s', service)) {
        sh "rm -rf ${WORKSPACE}/*"
    }

    stage(String.format('clone %s code', service)) {
        //check CODE
        echo "clone ${service} code, project=${project}, branch=${branch}"
        checkout([$class                           : 'GitSCM', branches: [[name: String.format('*/%s', branch)]],
                  doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [],
                  userRemoteConfigs                : [[url: String.format('http://dingmw@gitlab:8888/BeyondCMP/%sbocloud.%s.git', project, service)]]])
    }

    stage(String.format('build %s', service)) {
        // rpm-build构建
        sh "cd $WORKSPACE/\n" +
                "\n" +
                "rm -rf ~/rpmbuild/\n" +
                "\n" +
                "sed -i \"s/Release:.*/Release:                %(git rev-parse --short HEAD).%(date +%Y%m%d%H).$BUILD_ID/g\" bocloud_pontus.spec\n" +
                "\n" +
                "rpmbuild -bb bocloud_pontus.spec\n" +
                "\n" +
                "git checkout bocloud_pontus.spec\n" +
                "\n" +
                "ls ~/rpmbuild/RPMS/x86_64/bocloud_pontus-*.rpm"
    }

    stage(String.format('build %s docker image', service)) {
        if (!port.isEmpty()) {
            sh "/usr/bin/cp ~/docker/pontus/Dockerfile ~/docker/pontus/docker-entrypoint.sh ${WORKSPACE}"
            sh "/usr/bin/cp ~/docker/pontus/config.yml ${WORKSPACE}"
            sh "/usr/bin/cp ${WORKSPACE}/../../rpmbuild/RPMS/x86_64/bocloud_*.rpm ${WORKSPACE}"
            def dockerfile = 'Dockerfile'
            def customImage = docker.build("registry.bocloud.com.cn/cmp/bocloud.${service}:${v}",
                    "-f ${dockerfile} . --build-arg port=${port}")
            customImage.push()
        }
    }

    stage(String.format('clean %s', service)) {
        sh "rm -rf ${WORKSPACE}/*"
    }
}


def buildWeb(project, v, branch) {
    def cscUrl, cmcUrl
    if (project.equals("develop")) {
        cmcUrl = String.format('http://dingmw@gitlab:8888/BeyondCMP/%sweb/cmc-web.git', project)
        cscUrl = String.format('http://dingmw@gitlab:8888/BeyondCMP/%sweb/csc-web.git', project)
    } else {
        cmcUrl = String.format('http://dingmw@gitlab:8888/BeyondCMP/%scmc-web.git', project)
        cscUrl = String.format('http://dingmw@gitlab:8888/BeyondCMP/%scsc-web.git', project)
    }

    stage('prepare web') {
        sh "rm -rf ${WORKSPACE}/*"
    }
    stage('clone cmc-web code') {
        //check CODE
        echo "clone cmc-web code, project=${project}, branch=${branch}"
        checkout([$class                           : 'GitSCM', branches: [[name: String.format('*/%s', branch)]],
                  doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [],
                  userRemoteConfigs                : [[url: cmcUrl]]
        ])
    }

    stage('build cmc-web') {
        // 构建
        sh "rm -rf cmc-web"
        sh "/usr/local/nodejs/bin/yarn --ignore-engines"
        sh "/usr/local/nodejs/bin/yarn build"
        sh "mv dist cmc-web"
        sh "rm -rf `ls|egrep -v cmc-web`"
    }

    stage('clone csc-web code') {
        //check CODE
        echo "clone csc-web code, project=${project}, branch=${branch}"
        checkout([$class                           : 'GitSCM', branches: [[name: String.format('*/%s', branch)]],
                  doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [],
                  userRemoteConfigs                : [[url: cscUrl]]
        ])
    }

    stage('build csc-web') {
        // 构建
        sh "rm -rf csc-web"
        sh "/usr/local/nodejs/bin/yarn --ignore-engines"
        sh "/usr/local/nodejs/bin/yarn build"
        sh "mv dist csc-web"
        sh "rm -rf `ls|egrep -v '(csc-web|cmc-web)'`"
    }

    stage(String.format('build web docker image')) {
        sh "/usr/bin/cp ~/docker/web/Dockerfile ${WORKSPACE}"
        sh "/usr/bin/cp ~/docker/web/nginx.conf ${WORKSPACE}"
        def dockerfile = 'Dockerfile'
        def customImage = docker.build("registry.bocloud.com.cn/cmp/bocloud.web:${v}",
                "-f ${dockerfile} .")
        customImage.push()
    }

    stage('clean web') {
        sh "rm -rf ${WORKSPACE}/*"
    }
}

def version() {
    def matcher = readFile('pom.xml') =~ '<version>(.+)</version>'
    matcher ? matcher[1][1] : null
}


