pipeline {
    agent any

    options {
        timestamps()
        disableConcurrentBuilds()
    }

    parameters {
        choice(name: 'BUILD_PROFILE', choices: ['prod', 'local'], description: 'Maven profile')
        choice(name: 'BUILD_SCOPE', choices: ['all', 'gateway', 'auth', 'system', 'tools'], description: 'Build target')
        booleanParam(name: 'DEPLOY', defaultValue: true, description: 'Deploy to remote server')
        choice(name: 'DEPLOY_MODE', choices: ['local', 'ssh'], description: 'Deploy to local Jenkins server or remote SSH host')
        string(name: 'DEPLOY_DIR', defaultValue: '/zoumh/java/zmh/backend', description: 'Remote backend artifact directory')
        string(name: 'SSH_HOST', defaultValue: 'your.server.ip', description: 'Deploy host when DEPLOY_MODE=ssh')
        string(name: 'SSH_USER', defaultValue: 'root', description: 'Deploy user when DEPLOY_MODE=ssh')
        string(name: 'POST_DEPLOY_CMD', defaultValue: 'systemctl restart zmh-gateway zmh-auth zmh-system', description: 'Optional restart command after files are copied')
    }

    environment {
        MVN_CMD = 'mvn -U -DskipTests'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
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
                        tools  : '-pl zoumh-modules/zoumh-tools -am'
                    ]
                    def scopeArg = scopeMap[params.BUILD_SCOPE]
                    sh "${env.MVN_CMD} -P${params.BUILD_PROFILE} clean package ${scopeArg}"
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
                            mkdir -p "${DEPLOY_DIR}/packages"
                            cp -f ruoyi-gateway/target/*.jar "${DEPLOY_DIR}/packages/" || true
                            cp -f ruoyi-auth/target/*.jar "${DEPLOY_DIR}/packages/" || true
                            cp -f ruoyi-modules/ruoyi-system/target/*.jar "${DEPLOY_DIR}/packages/" || true
                            cp -f ruoyi-modules/ruoyi-gen/target/*.jar "${DEPLOY_DIR}/packages/" || true
                            cp -f ruoyi-modules/ruoyi-job/target/*.jar "${DEPLOY_DIR}/packages/" || true
                            cp -f ruoyi-modules/ruoyi-file/target/*.jar "${DEPLOY_DIR}/packages/" || true
                            cp -f zoumh-modules/zoumh-tools/target/*.jar "${DEPLOY_DIR}/packages/" || true
                            if [ -n "${POST_DEPLOY_CMD}" ]; then
                                sh -c "${POST_DEPLOY_CMD}"
                            fi
                        '''
                    } else {
                        sshagent(credentials: ['zoumh-ssh']) {
                            sh '''
                                set -e
                                ssh -o StrictHostKeyChecking=no ${SSH_USER}@${SSH_HOST} "mkdir -p ${DEPLOY_DIR}/packages"
                                scp -o StrictHostKeyChecking=no ruoyi-gateway/target/*.jar ${SSH_USER}@${SSH_HOST}:${DEPLOY_DIR}/packages/ || true
                                scp -o StrictHostKeyChecking=no ruoyi-auth/target/*.jar ${SSH_USER}@${SSH_HOST}:${DEPLOY_DIR}/packages/ || true
                                scp -o StrictHostKeyChecking=no ruoyi-modules/ruoyi-system/target/*.jar ${SSH_USER}@${SSH_HOST}:${DEPLOY_DIR}/packages/ || true
                                scp -o StrictHostKeyChecking=no ruoyi-modules/ruoyi-gen/target/*.jar ${SSH_USER}@${SSH_HOST}:${DEPLOY_DIR}/packages/ || true
                                scp -o StrictHostKeyChecking=no ruoyi-modules/ruoyi-job/target/*.jar ${SSH_USER}@${SSH_HOST}:${DEPLOY_DIR}/packages/ || true
                                scp -o StrictHostKeyChecking=no ruoyi-modules/ruoyi-file/target/*.jar ${SSH_USER}@${SSH_HOST}:${DEPLOY_DIR}/packages/ || true
                                scp -o StrictHostKeyChecking=no zoumh-modules/zoumh-tools/target/*.jar ${SSH_USER}@${SSH_HOST}:${DEPLOY_DIR}/packages/ || true
                                if [ -n "${POST_DEPLOY_CMD}" ]; then
                                    ssh -o StrictHostKeyChecking=no ${SSH_USER}@${SSH_HOST} "${POST_DEPLOY_CMD}"
                                fi
                            '''
                        }
                    }
                }
            }
        }
    }
}
