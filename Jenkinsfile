pipeline {
    agent any

    options {
        timestamps()
        disableConcurrentBuilds()
    }

    parameters {
        choice(name: 'BUILD_PROFILE', choices: ['prod', 'local'], description: 'Maven profile')
        choice(name: 'BUILD_SCOPE', choices: ['all', 'gateway', 'auth', 'system', 'tools', 'hotel'], description: 'Build target')
        booleanParam(name: 'DEPLOY', defaultValue: true, description: 'Deploy to remote server')
        choice(name: 'DEPLOY_MODE', choices: ['local', 'ssh'], description: 'Deploy to local Jenkins server or remote SSH host')
        string(name: 'DEPLOY_DIR', defaultValue: '/zoumh/java/zmh/backend', description: 'Remote backend artifact directory')
        string(name: 'LOG_DIR', defaultValue: '/zoumh/java/zmh/backend/logs', description: 'Remote backend log directory')
        string(name: 'RUN_DIR', defaultValue: '/zoumh/java/zmh/backend/run', description: 'Host pid/run directory')
        string(name: 'DEPLOY_SCRIPT_DIR', defaultValue: '/zoumh/java/zmh/backend/bin', description: 'Remote backend script directory')
        string(name: 'SSH_HOST', defaultValue: '156.225.28.110', description: 'Deploy host when DEPLOY_MODE=ssh')
        string(name: 'SSH_USER', defaultValue: 'root', description: 'Deploy user when DEPLOY_MODE=ssh')
        string(name: 'SSH_CREDENTIALS_ID', defaultValue: 'zoumh-ssh', description: 'SSH credentials id in Jenkins')
        string(name: 'MAVEN_REPO', defaultValue: '${JENKINS_HOME}/caches/maven', description: 'Persistent Maven dependency cache')
        string(name: 'JDK_ARCHIVE', defaultValue: '/zoumh/jdk/openjdk-21.0.2_linux-x64_bin.tar.gz', description: 'Host JDK 21 tarball path')
        string(name: 'JDK_HOME', defaultValue: '/zoumh/jdk/jdk21', description: 'Host JDK install path')
        string(name: 'POST_DEPLOY_CMD', defaultValue: '', description: 'Optional command after backend containers are recreated')
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Prepare Cache') {
            steps {
                sh '''
                    set -e
                    mkdir -p "${MAVEN_REPO}"
                '''
            }
        }

        stage('Build') {
            steps {
                script {
                    def scopeMap = [
                        all    : '',
                        gateway: '-pl ruoyi-gateway -am',
                        auth   : '-pl ruoyi-auth -am',
                        system : '-pl ruoyi-modules/ruoyi-system -am',
                        tools  : '-pl zoumh-modules/zoumh-tools -am',
                        hotel  : '-pl zoumh-modules/zoumh-hotel-monitor -am'
                    ]
                    def scopeArg = scopeMap[params.BUILD_SCOPE]
                    sh """
                        set -e
                        mvn -Dmaven.repo.local="${MAVEN_REPO}" -DskipTests -P${params.BUILD_PROFILE} clean package ${scopeArg}
                    """
                }
            }
        }

        stage('Archive') {
            steps {
                archiveArtifacts artifacts: '**/target/*.jar', fingerprint: true
            }
        }

        stage('Deploy') {
            when {
                expression { return params.DEPLOY }
            }
            steps {
                script {
                    if (params.DEPLOY_MODE == 'local') {
                        sh '''
                            set -e
                            mkdir -p "${DEPLOY_DIR}/packages" "${LOG_DIR}" "${RUN_DIR}" "${DEPLOY_SCRIPT_DIR}"
                            cp -f ruoyi-gateway/target/*.jar "${DEPLOY_DIR}/packages/" || true
                            cp -f ruoyi-auth/target/*.jar "${DEPLOY_DIR}/packages/" || true
                            cp -f ruoyi-modules/ruoyi-system/target/*.jar "${DEPLOY_DIR}/packages/" || true
                            cp -f ruoyi-modules/ruoyi-gen/target/*.jar "${DEPLOY_DIR}/packages/" || true
                            cp -f ruoyi-modules/ruoyi-job/target/*.jar "${DEPLOY_DIR}/packages/" || true
                            cp -f ruoyi-modules/ruoyi-file/target/*.jar "${DEPLOY_DIR}/packages/" || true
                            cp -f zoumh-modules/zoumh-tools/target/*.jar "${DEPLOY_DIR}/packages/" || true
                            cp -f zoumh-modules/zoumh-hotel-monitor/target/*.jar "${DEPLOY_DIR}/packages/" || true
                            cp -f scripts/deploy-backend-host.sh "${DEPLOY_SCRIPT_DIR}/deploy-backend-host.sh"
                            chmod +x "${DEPLOY_SCRIPT_DIR}/deploy-backend-host.sh"
                            PACKAGE_DIR="${DEPLOY_DIR}/packages" LOG_DIR="${LOG_DIR}" RUN_DIR="${RUN_DIR}" JDK_ARCHIVE="${JDK_ARCHIVE}" JDK_HOME="${JDK_HOME}" POST_DEPLOY_CMD="${POST_DEPLOY_CMD}" "${DEPLOY_SCRIPT_DIR}/deploy-backend-host.sh"
                        '''
                    } else {
                        sshagent(credentials: [params.SSH_CREDENTIALS_ID]) {
                            sh '''
                                set -e
                                ssh -o StrictHostKeyChecking=no ${SSH_USER}@${SSH_HOST} "mkdir -p ${DEPLOY_DIR}/packages ${LOG_DIR} ${RUN_DIR} ${DEPLOY_SCRIPT_DIR}"
                                scp -o StrictHostKeyChecking=no ruoyi-gateway/target/*.jar ${SSH_USER}@${SSH_HOST}:${DEPLOY_DIR}/packages/ || true
                                scp -o StrictHostKeyChecking=no ruoyi-auth/target/*.jar ${SSH_USER}@${SSH_HOST}:${DEPLOY_DIR}/packages/ || true
                                scp -o StrictHostKeyChecking=no ruoyi-modules/ruoyi-system/target/*.jar ${SSH_USER}@${SSH_HOST}:${DEPLOY_DIR}/packages/ || true
                                scp -o StrictHostKeyChecking=no ruoyi-modules/ruoyi-gen/target/*.jar ${SSH_USER}@${SSH_HOST}:${DEPLOY_DIR}/packages/ || true
                                scp -o StrictHostKeyChecking=no ruoyi-modules/ruoyi-job/target/*.jar ${SSH_USER}@${SSH_HOST}:${DEPLOY_DIR}/packages/ || true
                                scp -o StrictHostKeyChecking=no ruoyi-modules/ruoyi-file/target/*.jar ${SSH_USER}@${SSH_HOST}:${DEPLOY_DIR}/packages/ || true
                                scp -o StrictHostKeyChecking=no zoumh-modules/zoumh-tools/target/*.jar ${SSH_USER}@${SSH_HOST}:${DEPLOY_DIR}/packages/ || true
                                scp -o StrictHostKeyChecking=no zoumh-modules/zoumh-hotel-monitor/target/*.jar ${SSH_USER}@${SSH_HOST}:${DEPLOY_DIR}/packages/ || true
                                scp -o StrictHostKeyChecking=no scripts/deploy-backend-host.sh ${SSH_USER}@${SSH_HOST}:${DEPLOY_SCRIPT_DIR}/deploy-backend-host.sh
                                ssh -o StrictHostKeyChecking=no ${SSH_USER}@${SSH_HOST} "chmod +x ${DEPLOY_SCRIPT_DIR}/deploy-backend-host.sh && PACKAGE_DIR=${DEPLOY_DIR}/packages LOG_DIR=${LOG_DIR} RUN_DIR=${RUN_DIR} JDK_ARCHIVE=${JDK_ARCHIVE} JDK_HOME=${JDK_HOME} POST_DEPLOY_CMD='${POST_DEPLOY_CMD}' ${DEPLOY_SCRIPT_DIR}/deploy-backend-host.sh"
                            '''
                        }
                    }
                }
            }
        }
    }
}
