#!groovy
pipeline {
    agent any

    environment {
        COMMIT_ID = sh(script: "git log -1 --pretty='%h'", returnStdout: true).trim()
        COMMIT_LOGS = sh(script: "git log -1 --pretty='%s'", returnStdout: true).trim()
        AUTHORS = sh(script: "git log --oneline -1 --format=%an", returnStdout: true).trim()
        FILE_CHANGES = sh(script: "git log -1 --pretty=tformat: --numstat | gawk '{ add += \$1 ; subs += \$2 ;} END { printf \"增加%s行, 删除%s行\",add,subs }'", returnStdout: true).trim()
        WECHAT_TOKEN = credentials("BUILD_NOTICE_WECHAT_TOKEN")
    }

    stages {
        stage('重置环境') {
            steps {
                sh "git checkout . && git clean -xdf"
            }
        }

        stage('编译代码') {
            steps {
                sh "source /etc/profile && mvn -T 1C -U -am clean deploy -DskipTests"
            }
        }
    }

    post {
        always {
            deleteDir()
            echo "[${JOB_NAME}]部署结果[${currentBuild.result}]"
            echo "作者：${AUTHORS}\n内容：${COMMIT_LOGS}\n变更：${FILE_CHANGES}"
            sh """curl -X POST -H 'Content-Type: application/json' -d '{"msgtype": "markdown","markdown": {"content": "jenkins：${JOB_NAME}(${currentBuild.result})\\n> 作者：${AUTHORS}\\n> 内容：${COMMIT_LOGS}\\n> 变更：${FILE_CHANGES}\\n> 链接：[jenkins](${BUILD_URL}console)"}}' https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=${WECHAT_TOKEN}"""
        }
    }
}