<div align="center">

# CloudDevOpsProject — End-to-End DevSecOps, GitOps, Ansible & AWS Terraform Platform

**Complete Cloud-Native Microservices Lifecycle: Local Docker Compose Stack, Modular AWS Terraform Provisioning, Dynamic-Inventory Ansible Configuration, DevSecOps Jenkins Shared Library, and Loop-Free Argo CD GitOps on Kubernetes**

![Docker](https://img.shields.io/badge/Docker-Compose_%26_Multi--Service-2496ED?style=for-the-badge&logo=docker&logoColor=white)
![Terraform](https://img.shields.io/badge/Terraform-Modular_IaC-7B42BC?style=for-the-badge&logo=terraform&logoColor=white)
![Ansible](https://img.shields.io/badge/Ansible-Roles_%26_Dynamic_Inv-EE0000?style=for-the-badge&logo=ansible&logoColor=white)
![Jenkins](https://img.shields.io/badge/Jenkins-Shared_Library-D24939?style=for-the-badge&logo=jenkins&logoColor=white)
![Trivy](https://img.shields.io/badge/Trivy-Security_Gate-1904DA?style=for-the-badge&logo=aquasecurity&logoColor=white)
![Kubernetes](https://img.shields.io/badge/Kubernetes-K3s_%2F_EKS-326CE5?style=for-the-badge&logo=kubernetes&logoColor=white)
![ArgoCD](https://img.shields.io/badge/Argo_CD-GitOps_Sync-EF7B4D?style=for-the-badge&logo=argo&logoColor=white)

</div>

---

## 1. Executive Summary & End-to-End Lifecycle

**CloudDevOpsProject** transforms a three-tier microservices application (**Frontend**, **Auth Service**, and **Roadmap Service**) backed by **MySQL 8.0** into an automated, production-ready **DevSecOps, GitOps, and Infrastructure as Code (IaC)** platform.

The architecture covers the entire cloud engineering lifecycle:
1. **Repository Onboarding & Local Containerization (`docker-compose.yml`):** Onboarded the upstream application source (`IbrahimAdel15/iVolveFinalProject`), decoupled Git history into `YosraMamdouh/CloudDevOpsProject`, containerized each service via dedicated `Dockerfile` definitions, and built `docker-compose.yml` for local multi-container orchestration and database testing.
2. **Modular AWS Infrastructure Provisioning (`terraform/`):** Provisioned VPC networking (Public/Private Subnets, IGW, NAT Gateway, Network ACLs), Jenkins EC2 compute, Multi-AZ EKS cluster, and ECR repositories using four reusable Terraform modules backed by an **Amazon S3 Remote State Backend**.
3. **Automated Configuration Management (`ansible/`):** Configured the Jenkins EC2 server automatically using an **AWS EC2 Dynamic Inventory** (`aws_ec2.yml`), master playbook (`jenkins-setup.yml`), and four modular **Ansible Roles** (`java`, `jenkins`, `docker`, `trivy`).
4. **Centralized CI/CD & DevSecOps (`vars/ivolvePipeline.groovy`):** Engineered a DRY **Jenkins Shared Library** that executes automated tests, builds immutable Docker images, audits vulnerabilities with **Aqua Trivy**, pushes verified artifacts to the container registry, updates `k8s/*.yaml` tags with loop-prevention guards, and dispatches HTML email alerts.
5. **Declarative Kubernetes & GitOps Delivery (`k8s/` & `argocd/`):** Deployed workloads into the **`ivolve`** namespace (`StorageClass`, `ConfigMap`, `Secret`, `MySQL StatefulSet` + Headless Service, `2-replica` service Deployments, and `Nginx Ingress`) synchronized continuously via **Argo CD**.

---

## 2. System Architecture & Data Flow

    [Upstream Repo: iVolveFinalProject] ──(git clone & decouple)──▶ [YosraMamdouh/CloudDevOpsProject (main)]
                                                                               │
         ┌─────────────────────────────────────────────────────────────────────┴─────────────────────────────────────────────────────────────────────┐
         ▼                                                                                                                                           ▼
    [INFRASTRUCTURE & CONFIG MANAGEMENT]                                                                                       [CI/CD & GITOPS DELIVERY PIPELINE]
    Terraform (S3 State Backend)                                                                                               GitHub Webhook (Ngrok) ──▶ Jenkins Shared Library
      ├── Network Module (VPC, Subnets, IGW, NAT, NACLs)                                                                         ├── 1. Author Guard ('Jenkins CI') & Workspace Reset
      ├── Server Module  (Jenkins EC2 + Security Groups) ──(AWS Dynamic Inventory)──▶ Ansible Playbook (jenkins-setup.yml)       ├── 2. Smoke & Syntax Verification
      ├── EKS Module     (Cluster + 2 Multi-AZ Nodes)                                   ├── Role: java                           ├── 3. Docker Build (Immutable BUILD_NUMBER Tag)
      └── ECR Module     (3 Private Repositories)                                       ├── Role: jenkins                        ├── 4. Trivy Security Audit (HIGH / CRITICAL CVEs)
                                                                                        ├── Role: docker                         ├── 5. Push to Registry & Local Image Prune
                                                                                        └── Role: trivy                          ├── 6. Mutate k8s/*.yaml & Push '[skip ci]' Commit
                                                                                                                                 └── 7. SMTP HTML Build Notification
                                                                                                                                                     │
                                                                                                                                           (GitOps Auto-Sync)
                                                                                                                                                     ▼
                                                                                                                                      Argo CD Application (argocd/application.yaml)
                                                                                                                                                     │
                                                                                                                                                     ▼
                                                                                                                                      Kubernetes Cluster (ivolve namespace)
                                                                                                                                        ├── Nginx Ingress ──▶ Frontend (:3000, 2 Pods)
                                                                                                                                        ├── Auth Service (:5000, 2 Pods)
                                                                                                                                        ├── Roadmap Service (:8080, 2 Pods)
                                                                                                                                        └── MySQL 8.0 StatefulSet + Headless Svc + PVC

---

## 3. Component & Deliverables Breakdown

### A. Local Multi-Container Stack (`docker-compose.yml`)
* Connects **Frontend**, **Auth Service**, **Roadmap Service**, and **MySQL 8.0** on an isolated Docker bridge network with environment variables, health checks, and persistent database volume mounting for rapid local verification.

### B. Modular AWS Terraform Infrastructure (`terraform/`)
| Module / File | Path | Provisioned Resources & Capabilities |
| :--- | :--- | :--- |
| **S3 Backend** | `terraform/backend.tf` | Centralized remote state storage (`prod/terraform.tfstate`) in Amazon S3 (validated locally via LocalStack). |
| **Network** | `terraform/modules/network/` | VPC (`10.0.0.0/16`), 2 Public & 2 Private Subnets (`us-east-1a`, `us-east-1b`), IGW, NAT Gateway + EIP, Route Tables, and Public/Private **Network ACLs (NACLs)**. |
| **Server** | `terraform/modules/server/` | Jenkins EC2 instance (`t3.medium`) in the public subnet with hardened Security Groups (`22`, `8080`). |
| **EKS** | `terraform/modules/eks/` | Managed EKS cluster, IAM roles/policies, and a 2-node Worker Node Group distributed across separate private subnets and Availability Zones. |
| **ECR** | `terraform/modules/ecr/` | Private container repositories (`ivolve/frontend`, `ivolve/auth-service`, `ivolve/roadmap-service`) with `scan_on_push = true`. |

### C. Ansible Configuration Management (`ansible/`)
* **Dynamic Inventory (`ansible/inventory/aws_ec2.yml`):** Automatically discovers EC2 instances tagged for Jenkins via the `amazon.aws.aws_ec2` plugin without hardcoded IP addresses.
* **Master Playbook (`ansible/jenkins-setup.yml`) & Roles (`ansible/roles/`):**
  * `roles/java`: Installs OpenJDK runtime required by Jenkins.
  * `roles/jenkins`: Configures Jenkins repositories, installs the Jenkins service, and enables daemon startup.
  * `roles/docker`: Installs Docker Engine, configures group permissions for `jenkins`, and starts the Docker daemon.
  * `roles/trivy`: Installs Aqua Trivy vulnerability scanner for CI/CD container security auditing.

### D. Jenkins Shared Library CI/CD (`vars/ivolvePipeline.groovy`)
* **Stages Executed:** `Checkout & Anti-Loop Guard` -> `Smoke Tests` -> `Build Image` -> `Scan Image (Trivy)` -> `Push Image` -> `Delete Image Locally` -> `Update Manifests (sed)` -> `Push Manifests ([skip ci])` -> `SMTP HTML Alerting`.
* **Anti-Loop & Concurrency Engineering:** Prevents infinite webhook loops via commit-author inspection (`Jenkins CI`) and `[skip ci]` tags, while executing `git fetch --all && git reset --hard origin/main` to prevent concurrent workspace collisions.

### E. Kubernetes & Argo CD GitOps (`k8s/` & `argocd/`)
| Manifest | Kubernetes Resource | Purpose |
| :--- | :--- | :--- |
| `k8s/01-namespace.yaml` | `Namespace` (`ivolve`) | Isolates all application workloads and policies. |
| `k8s/02-storageclass.yaml` | `StorageClass` | Dynamic storage provisioning for stateful database persistence. |
| `k8s/03-configmap-secret.yaml` | `ConfigMap` & `Secret` | Externalizes service endpoints and encrypted database credentials. |
| `k8s/04-mysql-statefulset.yaml` | `StatefulSet` & `Headless Service` | Runs `mysql-0` with stable network identity and `PersistentVolumeClaim`. |
| `k8s/05-auth-service.yaml` | `Deployment` (2x) & `Service` | Authentication microservice (`:5000`) with zero-downtime rolling updates. |
| `k8s/06-roadmap-service.yaml` | `Deployment` (2x) & `Service` | Roadmap REST API (`:8080`) with 2 replicas. |
| `k8s/07-frontend.yaml` | `Deployment` (2x) & `Service` | Frontend web UI (`:3000`) with 2 replicas. |
| `k8s/08-ingress.yaml` | `Ingress` | Routes external HTTP traffic (`/`, `/api/auth`, `/api/roadmap`) into cluster services. |
| `argocd/application.yaml` | Argo CD `Application` | Continuously reconciles `k8s/` from `main` with `automated` prune & self-heal enabled. |

---

## 4. Repository Structure

    CloudDevOpsProject/
    ├── ansible/                      # Configuration management with Dynamic Inventory & Roles
    │   ├── ansible.cfg               # Ansible defaults & dynamic inventory plugin configuration
    │   ├── jenkins-setup.yml         # Master playbook configuring the Jenkins EC2 host
    │   ├── inventory/
    │   │   └── aws_ec2.yml           # AWS EC2 Dynamic Inventory configuration
    │   └── roles/
    │       ├── docker/               # Installs & configures Docker Engine
    │       ├── java/                 # Installs OpenJDK runtime
    │       ├── jenkins/              # Installs & enables Jenkins CI server
    │       └── trivy/                # Installs Aqua Trivy security scanner
    ├── argocd/
    │   └── application.yaml          # Declarative Argo CD Application manifest (Auto-Sync)
    ├── auth-service/                 # Auth microservice source, Dockerfile, and Jenkinsfile
    ├── frontend/                     # Frontend application source, Dockerfile, and Jenkinsfile
    ├── roadmap-service/            # Roadmap API source, Dockerfile, and Jenkinsfile
    ├── k8s/                          # Declarative Kubernetes manifests (ivolve namespace)
    │   ├── 01-namespace.yaml
    │   ├── 02-storageclass.yaml
    │   ├── 03-configmap-secret.yaml
    │   ├── 04-mysql-statefulset.yaml
    │   ├── 05-auth-service.yaml
    │   ├── 06-roadmap-service.yaml
    │   ├── 07-frontend.yaml
    │   └── 08-ingress.yaml
    ├── terraform/                    # Modular AWS Infrastructure as Code (S3 Remote Backend)
    │   ├── backend.tf
    │   ├── main.tf
    │   ├── outputs.tf
    │   ├── provider.tf
    │   ├── variables.tf
    │   └── modules/
    │       ├── ecr/
    │       ├── eks/
    │       ├── network/
    │       └── server/
    ├── vars/
    │   └── ivolvePipeline.groovy     # Global Jenkins Shared Library declarative pipeline
    ├── docker-compose.yml            # Local multi-container stack (3 microservices + MySQL)
    ├── .gitignore                    # Excludes .terraform/, *.tfstate, and local overrides
    └── README.md                     # Complete project architecture & operational guide

---

## 5. Setup & Verification Runbook

### 1. Local Docker Compose Verification
    docker compose up -d --build
    docker compose ps

### 2. Terraform IaC Validation
    cd terraform
    terraform init
    terraform validate
    terraform plan

### 3. Ansible Configuration Execution (Dynamic Inventory)
    cd ansible
    ansible-inventory -i inventory/aws_ec2.yml --graph
    ansible-playbook -i inventory/aws_ec2.yml jenkins-setup.yml

### 4. Kubernetes & Argo CD Verification
    kubectl apply -f argocd/application.yaml
    kubectl get pods -n ivolve -o wide
    kubectl get applications -n argocd

---

<div align="center">

**Engineered by [Yousra Mamdouh](https://github.com/YosraMamdouh) — Cloud & DevOps Engineering Portfolio**

</div>
