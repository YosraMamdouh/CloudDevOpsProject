output "vpc_id" {
  description = "Provisioned VPC ID"
  value       = module.network.vpc_id
}

output "jenkins_public_ip" {
  description = "Public IP address of the Jenkins EC2 server"
  value       = module.server.jenkins_public_ip
}

output "eks_cluster_name" {
  description = "Provisioned EKS Cluster Name"
  value       = module.eks.cluster_name
}

output "eks_cluster_endpoint" {
  description = "EKS Kubernetes API Server Endpoint"
  value       = module.eks.cluster_endpoint
}

output "ecr_repository_urls" {
  description = "Map of created ECR repository URLs"
  value       = module.ecr.repository_urls
}
