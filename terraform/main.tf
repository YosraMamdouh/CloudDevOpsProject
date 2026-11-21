module "network" {
  source               = "./modules/network"
  project_name         = var.project_name
  vpc_cidr             = var.vpc_cidr
  public_subnet_cidrs  = var.public_subnet_cidrs
  private_subnet_cidrs = var.private_subnet_cidrs
  availability_zones   = var.availability_zones
}

module "server" {
  source           = "./modules/server"
  project_name     = var.project_name
  vpc_id           = module.network.vpc_id
  public_subnet_id = module.network.public_subnet_ids[0]
  instance_type    = var.jenkins_instance_type
  key_name         = var.key_name
}

module "eks" {
  source             = "./modules/eks"
  project_name       = var.project_name
  vpc_id             = module.network.vpc_id
  private_subnet_ids = module.network.private_subnet_ids
  node_instance_type = var.eks_node_instance_type
}

module "ecr" {
  source       = "./modules/ecr"
  project_name = var.project_name
  repositories = var.ecr_repositories
}
