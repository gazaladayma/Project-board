# Project Board DevSecOps – Screenshot Evidence

This directory contains selected screenshots documenting the Project Board DevSecOps project.

The screenshots are organized by implementation area. They are provided as historical project evidence; AWS resources shown in these screenshots are not necessarily still running.

## 1. Terraform and AWS

| Screenshot | Evidence |
|---|---|
| `P5-00-terraform-plan-review.png.png` | Terraform plan review |
| `P5-00-terraform-apply-success.png.png` | Terraform apply completion |
| `P5-00-terraform-security-group.png.png` | AWS security group configuration |

## 2. GitHub

| Screenshot | Evidence |
|---|---|
| `P5-01-github-fork.png.png` | Project repository fork |
| `P5-02-repository-cloned.png.png` | Repository cloned locally |
| `P5-03-repository-inspection.png.png` | Local repository inspection |

## 3. Jenkins CI/CD

| Screenshot | Evidence |
|---|---|
| `01-jenkins-pipeline-success.png.png` | Jenkins pipeline success |
| `02-jenkins-console-success.png.png` | Jenkins console output |
| `P5-03-jenkins-pipeline.png.png` | Jenkins pipeline configuration or execution |
| `P5-02-jenkins-compile-test-success.png.png` | Compile and test stage success |
| `P5-19-Jenkins-Automatic-EKS-Deployment.png.png` | Automated EKS deployment evidence |

## 4. Maven Testing and Code Coverage

| Screenshot | Evidence |
|---|---|
| `P5-05-baseline-maven-tests.png.png` | Maven test execution |
| `Jacoco-Maven-build-success.png` | JaCoCo and Maven build evidence |

## 5. SonarQube

| Screenshot | Evidence |
|---|---|
| `P5-17-SonarQube-Quality-Gate-Passed.png.png` | SonarQube quality gate result |
| `P5-16-SonarQube-New-Code-Coverage-95-5.png.png` | New-code coverage result |
| `P5-SONARQUBE-Zero-Issues.png.png` | SonarQube issue summary |

## 6. Trivy Security Scanning

| Screenshot | Evidence |
|---|---|
| `Trivy-scan-successful.png` | Trivy image or project scan evidence |

## 7. Docker and Amazon ECR

| Screenshot | Evidence |
|---|---|
| `1-Docker-build-successful.png` | Docker image build |
| `Docker-image-in-ECR-Repository.png` | Image stored in Amazon ECR |

## 8. Amazon EKS and Kubernetes

| Screenshot | Evidence |
|---|---|
| `P5-20-EKS-Rollout-Success.png.png` | Kubernetes rollout result |
| `project5-eks-application-2-pods-ready.png.png` | Application pods readiness |
| `project5-aws-load-balancer-controller-installed.png.png` | AWS Load Balancer Controller installation evidence |

## 9. Deployed Application

| Screenshot | Evidence |
|---|---|
| `project5-boardgame-application-live-https.png.png` | Application accessed over HTTPS |
| `P5-21-Live-Homepage-Lavender-Theme.png.png` | Application homepage appearance |

## 10. Monitoring and Alerting

| Screenshot | Evidence |
|---|---|
| `Prometheus-Running.png` | Prometheus service running |
| `04-prometheus-targets-healthy.png.png` | Prometheus target health |
| `project5-grafana-eks-coredns-dashboard.png.png` | Grafana dashboard for EKS CoreDNS metrics |
| `GrafanaAlertsSuccessful-Slack.png` | Slack alert delivery evidence |

## Evidence and security notes

- These screenshots are selected from a larger local archive. The original archive is retained separately.
- Screenshots are historical evidence and do not establish that the application, Jenkins, SonarQube, EKS cluster, or monitoring services are currently available.
- Monitoring screenshots are included as project evidence; they should not be interpreted as proof that a separate monitoring environment is integrated with this application.
- Credentials, access tokens, private keys, webhook URLs, and other secrets must not be published. Review screenshots before making the repository public.
- Descriptions are based on screenshot filenames and have not been independently verified against every image.
