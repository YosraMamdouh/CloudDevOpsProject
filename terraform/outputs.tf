output "jenkins_public_ip" {
  value = module.server.jenkins_public_ip
}

output "eks_cluster_name" {
  value = module.eks.cluster_name
}

output "ecr_repositories" {
  value = module.ecr.repository_urls
}
