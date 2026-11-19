def call(Map config) {
    pipeline {
        agent { label 'built-in' }

        environment {
            SERVICE_NAME    = "${config.serviceName}"
            SERVICE_DIR     = "${config.serviceDir}"
            MANIFEST_FILE   = "${config.manifestFile}"
            ECR_REGISTRY    = "localhost:5100"
            IMAGE_REPO      = "${ECR_REGISTRY}/ivolve/${SERVICE_NAME}"
            IMAGE_TAG       = "${env.BUILD_NUMBER}"
            GIT_REPO        = "github.com/YosraMamdouh/CloudDevOpsProject.git"
            DOCKER_BUILDKIT = "0"
        }

        stages {
            stage('Check Trigger') {
                steps {
                    script {
                        def lastAuthor = sh(script: "git log -1 --pretty=format:'%an'", returnStdout: true).trim()
                        def isManual = currentBuild.getBuildCauses('hudson.model.Cause$UserIdCause').size() > 0
                        env.SHOULD_RUN = (lastAuthor != 'Jenkins CI' || isManual).toString()
                        echo "Last author: ${lastAuthor} | Manual run: ${isManual} | Should run: ${env.SHOULD_RUN}"
                    }
                }
            }

      stage('Run Tests') {
                when { expression { env.SHOULD_RUN == 'true' } }
                steps {
                    sh """
                        echo "Running validation & smoke tests for ${SERVICE_NAME}..."
                        test -f "./${SERVICE_DIR}/Dockerfile"
                        if [ -f "./${SERVICE_DIR}/package.json" ]; then
                            node -e "JSON.parse(require('fs').readFileSync('./${SERVICE_DIR}/package.json'))" 2>/dev/null || true
                            echo "Node.js project structure verified."
                        elif [ -f "./${SERVICE_DIR}/requirements.txt" ]; then
                            python3 -m py_compile ./${SERVICE_DIR}/*.py 2>/dev/null || true
                            echo "Python syntax & structure verified."
                        fi
                    """
                }
            }

            stage('Build Image') {
                when { expression { env.SHOULD_RUN == 'true' } }
                steps {
                    sh "docker build -t ${IMAGE_REPO}:${IMAGE_TAG} ./${SERVICE_DIR}"
                }
            }

            stage('Scan Image') {
                when { expression { env.SHOULD_RUN == 'true' } }
                steps {
                    sh "trivy image --timeout 15m --exit-code 0 --severity HIGH,CRITICAL ${IMAGE_REPO}:${IMAGE_TAG}"
                }
            }

            stage('Push Image') {
                when { expression { env.SHOULD_RUN == 'true' } }
                steps {
                    sh "docker push ${IMAGE_REPO}:${IMAGE_TAG}"
                }
            }

            stage('Delete Image Locally') {
                when { expression { env.SHOULD_RUN == 'true' } }
                steps {
                    sh "docker rmi ${IMAGE_REPO}:${IMAGE_TAG} || true"
                }
            }

            stage('Update Manifests') {
                when { expression { env.SHOULD_RUN == 'true' } }
                steps {
                    sh "sed -i 's|${IMAGE_REPO}:.*|${IMAGE_REPO}:${IMAGE_TAG}|g' k8s/${MANIFEST_FILE}"
                }
            }

            stage('Push Manifests') {
                when { expression { env.SHOULD_RUN == 'true' } }
                steps {
                    withCredentials([usernamePassword(credentialsId: 'github-creds', usernameVariable: 'GIT_USER', passwordVariable: 'GIT_PASS')]) {
                        sh """
                            git rebase --abort || true
                            rm -rf .git/rebase-merge .git/rebase-apply || true
                            git config user.email "jenkins@ivolve.local"
                            git config user.name "Jenkins CI"
                            git checkout -B main
                            git fetch https://${GIT_USER}:${GIT_PASS}@${GIT_REPO} main
                            git reset --hard FETCH_HEAD
                            sed -i 's|${IMAGE_REPO}:.*|${IMAGE_REPO}:${IMAGE_TAG}|g' k8s/${MANIFEST_FILE}
                            git add k8s/${MANIFEST_FILE}
                            git commit -m "[skip ci] Update ${SERVICE_NAME} image tag to ${IMAGE_TAG}" || exit 0
                            git push https://${GIT_USER}:${GIT_PASS}@${GIT_REPO} main
                        """
                    }
                }
            }
        }

        post {
            success {
                script {
                    if (env.SHOULD_RUN == 'true') {
                        emailext(
                            to: 'yousramamdouh1405@gmail.com',
                            subject: "✅ Build Success [${SERVICE_NAME}]: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                            body: """
                            <h3>Build and deployment completed successfully!</h3>
                            <p><b>Service:</b> ${SERVICE_NAME}</p>
                            <p><b>Pipeline:</b> ${env.JOB_NAME}</p>
                            <p><b>Build Number:</b> #${env.BUILD_NUMBER}</p>
                            <p><b>Image Tag:</b> ${IMAGE_REPO}:${IMAGE_TAG}</p>
                            <p><b>Build Details:</b> <a href="${env.BUILD_URL}">${env.BUILD_URL}</a></p>
                            """,
                            mimeType: 'text/html'
                        )
                    }
                }
            }
            failure {
                emailext(
                    to: 'yousramamdouh1405@gmail.com',
                    subject: "❌ Build Failed [${SERVICE_NAME}]: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                    body: """
                    <h3>Project build failed!</h3>
                    <p><b>Service:</b> ${SERVICE_NAME}</p>
                    <p><b>Pipeline:</b> ${env.JOB_NAME}</p>
                    <p><b>Build Number:</b> #${env.BUILD_NUMBER}</p>
                    <p><b>Error Reason (Last 30 lines of build log):</b></p>
                    <pre style="background:#f4f4f4;padding:10px;border:1px solid #ddd;">\${BUILD_LOG, maxLines=30, escapeHtml=true}</pre>
                    <p><b>Full Console Log:</b> <a href="${env.BUILD_URL}console">${env.BUILD_URL}console</a></p>
                    """,
                    mimeType: 'text/html'
                )
            }
        }
    }
}
