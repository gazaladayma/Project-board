pipeline {
    agent any

    tools {
        jdk 'jdk21'
        maven 'maven3'
    }

    stages {
        stage('Compile') {
            steps {
                sh 'mvn compile'
            }
        }

        stage('Test') {
            steps {
                sh 'mvn test'
            }
        }

        stage('SonarQube Analysis') {
            steps {
                withCredentials([string(
                    credentialsId: 'sonarqube-token',
                    variable: 'SONAR_TOKEN'
                )]) {
                    sh '''
                        mvn org.sonarsource.scanner.maven:sonar-maven-plugin:5.8.0.7211:sonar \
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
                    trivy fs \
                      --scanners vuln,misconfig,secret \
                      --format json \
                      --output trivy-fs-report.json \
                      .
                '''
            }
        }
    }

    post {
        always {
            archiveArtifacts artifacts: 'trivy-fs-report.json',
                             allowEmptyArchive: true,
                             fingerprint: true
        }
    }
}
