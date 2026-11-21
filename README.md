<div align="center">
# CloudDevOpsProject - End-to-End DevSecOps, GitOps & Terraform IaC Platform

**Automated Microservices CI/CD Pipeline with Security Vulnerability Auditing, Loop-Free GitOps Synchronization, Multi-Replica Kubernetes Orchestration, and Modular AWS Terraform Infrastructure**

![Jenkins](https://img.shields.io/badge/Jenkins-CI%2FCD-D24939?style=for-the-badge&logo=jenkins&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-Containers-2496ED?style=for-the-badge&logo=docker&logoColor=white)
![Trivy](https://img.shields.io/badge/Trivy-DevSecOps-1904DA?style=for-the-badge&logo=aquasecurity&logoColor=white)
![ArgoCD](https://img.shields.io/badge/Argo_CD-GitOps-EF7B4D?style=for-the-badge&logo=argo&logoColor=white)
![Kubernetes](https://img.shields.io/badge/Kubernetes-K3s-326CE5?style=for-the-badge&logo=kubernetes&logoColor=white)
![Terraform](https://img.shields.io/badge/Terraform-IaC-7B42BC?style=for-the-badge&logo=terraform&logoColor=white)
![AWS](https://img.shields.io/badge/AWS-Cloud-232F3E?style=for-the-badge&logo=amazonwebservices&logoColor=white)

</div>
---

## Overview

**CloudDevOpsProject** is a cloud-native, multi-tier microservices application engineered with an end-to-end **DevSecOps, GitOps, and Infrastructure as Code (IaC)** lifecycle. Every commit pushed to the repository triggers an isolated, parameterized **Jenkins Shared Library** pipeline that validates code integrity, builds immutable Docker images, audits containers for vulnerabilities using **Trivy**, pushes artifacts to a container registry, mutates declarative Kubernetes manifests, dispatches real-time HTML email alerts, and synchronizes cluster state via **Argo CD** with zero downtime. Underlying cloud infrastructure is provisioned declaratively using modular **Terraform** backed by remote **S3** state management.

---

## System Architecture & GitOps Workflow

    [Developer Push] --> [GitHub Repository (main)] --(Webhook via Ngrok)--> [Jenkins Shared Library]
                                                                                     |
         +---------------------------------------------------------------------------+
         v
    [Author Guard] -> [Tests] -> [Docker Build] -> [Trivy Scan] -> [Push Registry] -> [Update k8s/*.yaml] -> [SMTP Alert]
                                                                                              |
                                                                                     (GitOps Auto-Sync)
                                                                                              v
                                                                                  [Argo CD Controller (K3s)]
                                                                                              |
                                                    +---------------------+-------------------+------------------+
                                                    v                     v                                      v
                                             [Frontend (2x)]      [Auth Service (2x)]                 [Roadmap Service (2x)]
                                                                          |                                      |
                                                                          +--------------+-----------------------+
                                                                                         v
                                                                              [MySQL 8.0 StatefulSet]

---

## Microservices & Infrastructure Matrix

| Component | Architectural Role | Source Directory | Kubernetes Manifest | Container Registry & Port | Replicas |
| :--- | :--- | :--- | :--- | :--- | :---: |
| **Frontend UI** | Web Interface & Client Routing | `frontend/` | `k8s/07-frontend.yaml` | `localhost:5100/ivolve/frontend` (`:3000`) | `2` |
| **Auth Service** | Identity & Token Management | `auth-service/` | `k8s/05-auth.yaml` | `localhost:5100/ivolve/auth-service` (`:5000`) | `2` |
| **Roadmap Service** | Core Business Logic & REST API | `roadmap-service/` | `k8s/06-roadmap.yaml` | `localhost:5100/ivolve/roadmap-service` (`:8080`) | `2` |
| **MySQL Database** | Persistent Relational Storage | - | StatefulSet (`mysql-0`) | `mysql:8.0` (`:3306`) | `1` |
| **Shared Library** | DRY CI/CD Pipeline Engine | `vars/ivolvePipeline.groovy` | - | Parameterized Groovy DSL | - |

---

## AWS Infrastructure as Code (Modular Terraform)

Cloud infrastructure is structured into four reusable Terraform modules (`terraform/modules/`) backed by an **Amazon S3 remote state backend** (`terraform/backend.tf`):

1. **Network Module (`modules/network`):** Provisions an isolated VPC (`10.0.0.0/16`), dual-AZ Public and Private Subnets (`us-east-1a`, `us-east-1b`), Internet Gateway (IGW), NAT Gateway with Elastic IP, Route Tables, and stateless **Network ACLs (NACLs)** for public and private tiers.
2. **Server Module (`modules/server`):** Provisions an Ubuntu EC2 instance for **Jenkins** inside the public subnet with a hardened Security Group (`22`, `8080`) and automated `user_data` bootstrapping for Docker, Java 17, AWS CLI, and Jenkins.
3. **EKS Module (`modules/eks`):** Provisions an **Amazon EKS** cluster with IAM roles/policies and a Managed Node Group running **2 worker nodes** distributed across the private subnets in separate Availability Zones.
4. **ECR Module (`modules/ecr`):** Provisions private container repositories (`ivolve/frontend`, `ivolve/auth-service`, `ivolve/roadmap-service`) with automated `scan_on_push` vulnerability scanning enabled.

---

## Key Engineering Highlights

* **DRY Pipeline Architecture (Jenkins Shared Libraries):** Consolidated all build, validation, security scanning, registry push, and manifest-update stages inside `vars/ivolvePipeline.groovy`, keeping individual microservice `Jenkinsfile` definitions concise and declarative.
* **Loop-Free GitOps Automation:** Prevented recursive webhook build loops caused by Jenkins committing updated image tags back to `main` through commit-author verification (`Jenkins CI`), `[skip ci]` commit messages, and clean workspace synchronization (`git fetch` + `git reset --hard`) across concurrent runs.
* **Shift-Left Container Security (DevSecOps):** Embedded **Trivy** vulnerability scanning before registry publication to audit OS packages and application dependencies for `HIGH` and `CRITICAL` CVEs.
* **Zero-Downtime Rolling Deployments:** Configured **Argo CD** automated drift detection and self-healing against multi-replica Kubernetes Deployments (`2/2` running pods per service) to guarantee continuous availability during rollouts.
* **Actionable Build Observability:** Integrated SMTP HTML email notifications (`emailext`) delivering build metadata, tagged image references, direct console links, and the trailing 30 lines of error logs on build failures.

---

## Repository Structure

    CloudDevOpsProject/
    ├── auth-service/               # Authentication service source, Dockerfile, and Jenkinsfile
    ├── frontend/                   # Frontend application source, Dockerfile, and Jenkinsfile
    ├── roadmap-service/            # Roadmap API source, Dockerfile, and Jenkinsfile
    ├── k8s/                        # Declarative Kubernetes manifests (Deployments, Services, Ingress, Secrets)
    │   ├── 05-auth.yaml
    │   ├── 06-roadmap.yaml
    │   └── 07-frontend.yaml
    ├── terraform/                  # Modular AWS Infrastructure as Code (S3 Remote Backend)
    │   ├── backend.tf
    │   ├── main.tf
    │   ├── outputs.tf
    │   ├── provider.tf
    │   ├── variables.tf
    │   └── modules/
    │       ├── ecr/                # ECR container repositories
    │       ├── eks/                # EKS cluster & multi-AZ private worker nodes
    │       ├── network/            # VPC, Public/Private Subnets, IGW, NAT, NACLs
    │       └── server/             # Jenkins EC2 instance & Security Groups
    └── vars/
        └── ivolvePipeline.groovy   # Global Jenkins Shared Library declarative pipeline

---

## Cluster Verification & IaC Quickstart

### 1. Kubernetes & Argo CD Runtime Verification
    kubectl get pods -n ivolve -o wide
    kubectl describe pods -n ivolve | grep -i "Image:"
    kubectl port-forward -n ivolve svc/frontend 3000:3000 &
    kubectl port-forward -n ivolve svc/auth-service 5000:5000 &
    kubectl port-forward -n ivolve svc/roadmap-service 8082:8080 &

### 2. Terraform Infrastructure Validation
    cd terraform
    terraform init
    terraform validate
    terraform plan
