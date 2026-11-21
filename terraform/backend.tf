terraform {
  backend "s3" {
    bucket  = "ivolve-clouddevops-tfstate-bucket"
    key     = "prod/terraform.tfstate"
    region  = "us-east-1"
    encrypt = true
  }
}
