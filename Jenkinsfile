pipeline {
    agent any

    tools {
        jdk 'jdk21'
        maven 'maven3'
    }

    environment {
        AWS_REGION = 'us-east-1'
        ECR_REGISTRY = '820242917252.dkr.ecr.us-east-1.amazonaws.com'
        ECR_REPOSITORY = 'project-board'
    }

    stages {
        stage('Prepare kubectl') {
            steps {
                sh '''
                    set -eu

                    KUBECTL_VERSION="v1.37.0"
                    TOOLS_DIR="${WORKSPACE}/.tools"
                    KUBECTL_BIN="${TOOLS_DIR}/kubectl"

                    mkdir -p "${TOOLS_DIR}"

                    curl --fail --silent --show-error --location \
                      "https://dl.k8s.io/release/${KUBECTL_VERSION}/bin/linux/amd64/kubectl" \
                      --output "${KUBECTL_BIN}"

                    curl --fail --silent --show-error --location \
                      "https://dl.k8s.io/release/${KUBECTL_VERSION}/bin/linux/amd64/kubectl.sha256" \
                      --output "${TOOLS_DIR}/kubectl.sha256"

                    printf '%s  %s\\n' \
                      "$(cat "${TOOLS_DIR}/kubectl.sha256")" \
                      "${KUBECTL_BIN}" | sha256sum --check -

                    chmod 0755 "${KUBECTL_BIN}"
                    "${KUBECTL_BIN}" version --client
                '''
            }
        }

        stage('Compile') {
            steps {
                sh 'mvn -B compile'
            }
        }

        stage('Test') {
            steps {
                sh 'mvn -B test'
            }
        }

        stage('JaCoCo Report') {
            steps {
                sh '''
                    set -eu
                    mvn -B jacoco:report
                    test -s target/site/jacoco/jacoco.xml
                    echo "JaCoCo XML report generated successfully."
                '''
            }
        }

        stage('SonarQube Analysis') {
            steps {
                withCredentials([string(
                    credentialsId: 'sonarqube-token',
                    variable: 'SONAR_TOKEN'
                )]) {
                    sh '''
                        set -eu

                        mvn -B org.sonarsource.scanner.maven:sonar-maven-plugin:5.8.0.7211:sonar \
                          -Dsonar.projectKey=project-board \
                          -Dsonar.host.url=http://10.50.11.227:9000 \
                          -Dsonar.token="$SONAR_TOKEN"
                    '''
                }
            }
        }

        stage('Trivy Filesystem Scan') {
            steps {
                sh '''
                    set -eu

                    trivy fs \
                      --scanners vuln,misconfig,secret \
                      --format json \
                      --output trivy-fs-report.json \
                      .
                '''
            }
        }

        stage('Prepare Image Tag') {
            steps {
                script {
                    env.IMAGE_TAG = sh(
                        script: 'git rev-parse --short=12 HEAD',
                        returnStdout: true
                    ).trim()

                    if (!(env.IMAGE_TAG ==~ /[0-9a-f]{12}/)) {
                        error('Could not determine a valid 12-character Git commit tag.')
                    }

                    echo "Image tag: ${env.IMAGE_TAG}"
                }
            }
        }

        stage('Build Docker Image') {
            steps {
                sh '''
                    set -eu

                    IMAGE_URI="${ECR_REGISTRY}/${ECR_REPOSITORY}:${IMAGE_TAG}"

                    docker build --pull --tag "${IMAGE_URI}" .
                    docker image inspect "${IMAGE_URI}" --format '{{.Id}}'
                '''
            }
        }

        stage('Trivy Image Scan') {
            steps {
                sh '''
                    set -eu

                    IMAGE_URI="${ECR_REGISTRY}/${ECR_REPOSITORY}:${IMAGE_TAG}"

                    trivy image \
                      --scanners vuln \
                      --severity HIGH,CRITICAL \
                      --exit-code 1 \
                      --format json \
                      --output trivy-image-report.json \
                      "${IMAGE_URI}"
                '''
            }
        }

        stage('Push Image to ECR') {
            steps {
                sh '''
                    set -eu

                    IMAGE_URI="${ECR_REGISTRY}/${ECR_REPOSITORY}:${IMAGE_TAG}"

                    aws sts get-caller-identity \
                      --region "${AWS_REGION}" \
                      --no-cli-pager

                    aws ecr get-login-password \
                      --region "${AWS_REGION}" \
                    | docker login \
                        --username AWS \
                        --password-stdin "${ECR_REGISTRY}"

                    docker push "${IMAGE_URI}"

                    echo "Successfully pushed: ${IMAGE_URI}"
                '''
            }
        }

        stage('Deploy to EKS') {
            steps {
                sh '''
                    set -eu

                    KUBECTL_BIN="${WORKSPACE}/.tools/kubectl"
                    IMAGE_URI="${ECR_REGISTRY}/${ECR_REPOSITORY}:${IMAGE_TAG}"
                    KUBECONFIG_FILE="${WORKSPACE}/.kube/config"

                    mkdir -p "${WORKSPACE}/.kube"

                    aws eks update-kubeconfig \
                      --name project-board-cluster \
                      --region "${AWS_REGION}" \
                      --kubeconfig "${KUBECONFIG_FILE}"

                    export KUBECONFIG="${KUBECONFIG_FILE}"

                    echo "Deploying image: ${IMAGE_URI}"

                    "${KUBECTL_BIN}" -n project-board \
                      set image deployment/boardgame-deployment \
                      boardgame="${IMAGE_URI}"

                    "${KUBECTL_BIN}" -n project-board \
                      rollout status deployment/boardgame-deployment \
                      --timeout=180s

                    ACTUAL_IMAGE=$("${KUBECTL_BIN}" -n project-board \
                      get deployment boardgame-deployment \
                      -o 'jsonpath={.spec.template.spec.containers[?(@.name=="boardgame")].image}')

                    test "${ACTUAL_IMAGE}" = "${IMAGE_URI}"

                    echo "Verified deployed image: ${ACTUAL_IMAGE}"
                '''
            }
        }
    }

    post {
        always {
            archiveArtifacts artifacts: 'trivy-fs-report.json,trivy-image-report.json',
                             allowEmptyArchive: true,
                             fingerprint: true
        }
    }
}
