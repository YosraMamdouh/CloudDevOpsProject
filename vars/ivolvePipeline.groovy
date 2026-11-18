def call(Map config) {
    pipeline {
        agent any

        environment {
            SERVICE_NAME = "${config.serviceName}"
            SERVICE_DIR  = "${config.serviceDir}"
            MANIFEST_FILE = "${config.manifestFile}"
            ECR_REGISTRY = "000000000000.dkr.ecr.us-east-1.localhost:5100"
            IMAGE_REPO   = "${ECR_REGISTRY}/ivolve/${SERVICE_NAME}"
            IMAGE_TAG    = "${env.BUILD_NUMBER}"
            GIT_REPO     = "github.com/YosraMamdouh/CloudDevOpsProject.git"
        }

        stages {
            stage('Build Image') {
                steps {
                    sh """
                        docker build -t ${IMAGE_REPO}:${IMAGE_TAG} ./${SERVICE_DIR}
                    """
                }
            }

            stage('Scan Image') {
                steps {
                    sh """
                        trivy image --exit-code 0 --severity HIGH,CRITICAL ${IMAGE_REPO}:${IMAGE_TAG}
                    """
                }
            }

            stage('Push Image') {
                steps {
                    sh """
                        aws --endpoint-url=http://localhost:4566 ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin ${ECR_REGISTRY}
                        docker push ${IMAGE_REPO}:${IMAGE_TAG}
                    """
                }
            }

            stage('Delete Image Locally') {
                steps {
                    sh """
                        docker rmi ${IMAGE_REPO}:${IMAGE_TAG} || true
                    """
                }
            }

            stage('Update Manifests') {
                steps {
                    sh """
                        sed -i "s|${IMAGE_REPO}:.*|${IMAGE_REPO}:${IMAGE_TAG}|g" k8s/${MANIFEST_FILE}
                    """
                }
            }

            stage('Push Manifests') {
                steps {
                    withCredentials([usernamePassword(credentialsId: 'github-creds', usernameVariable: 'GIT_USER', passwordVariable: 'GIT_PASS')]) {
                        sh """
                            git config user.email "jenkins@ivolve.local"
                            git config user.name "Jenkins CI"
                            git add k8s/${MANIFEST_FILE}
                            git commit -m "Update ${SERVICE_NAME} image tag to ${IMAGE_TAG}" || exit 0
                            git push https://${GIT_USER}:${GIT_PASS}@${GIT_REPO} HEAD:main
                        """
                    }
                }
            }
        }
    }
}
