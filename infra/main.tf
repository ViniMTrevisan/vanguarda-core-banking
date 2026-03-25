# Terraform Infrastructure for vanguarda-core-banking

terraform {
  required_version = ">= 1.0.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
    random = {
      source  = "hashicorp/random"
      version = "~> 3.5"
    }
  }

# Uncomment for remote state (recommended for production)
# backend "s3" {
#   bucket         = "your-terraform-state-bucket"
#   key            = "vanguarda-core-banking/terraform.tfstate"
#   region         = "us-east-1"
#   encrypt        = true
#   dynamodb_table = "terraform-locks"
# }
}

provider "aws" {
  region = var.aws_region
}

# Variables
variable "aws_region" {
  description = "AWS region"
  type        = string
  default     = "us-east-1"
}

variable "environment" {
  description = "Environment name (dev, staging, prod)"
  type        = string
  default     = "dev"
}

variable "app_name" {
  description = "Application name"
  type        = string
  default     = "vanguarda-core-banking"
}

variable "jwt_secret" {
  description = "JWT signing secret (base64-encoded, min 32 bytes decoded)"
  type        = string
  sensitive   = true
  default     = "dmFuZ3VhcmRhLWp3dC1zZWNyZXQtcHJvZHVjYW8tY2hhbmdlLW1lLW5vdy0xMjM0NTY3ODk="
}

variable "auth_client_id" {
  description = "API auth client ID"
  type        = string
  default     = "vcb-admin"
}

variable "auth_client_secret" {
  description = "API auth client secret"
  type        = string
  sensitive   = true
  default     = "vcb-secret-change-me"
}

# VPC
module "vpc" {
  source = "./modules/vpc"

  app_name    = var.app_name
  environment = var.environment
}

# RDS PostgreSQL
module "rds" {
  source = "./modules/rds"

  app_name          = var.app_name
  environment       = var.environment
  vpc_id            = module.vpc.vpc_id
  subnet_ids        = module.vpc.private_subnet_ids
  security_group_id = module.vpc.db_security_group_id
  db_name           = "vcb_db"
  db_username       = "vcb"
}

# ElastiCache Redis
module "elasticache" {
  source = "./modules/elasticache"

  app_name          = var.app_name
  environment       = var.environment
  subnet_ids        = module.vpc.private_subnet_ids
  security_group_id = module.vpc.elasticache_security_group_id
}

# Amazon MQ (RabbitMQ)
module "amazonmq" {
  source = "./modules/amazonmq"

  app_name          = var.app_name
  environment       = var.environment
  subnet_ids        = module.vpc.private_subnet_ids
  security_group_id = module.vpc.amazonmq_security_group_id
}

# SSM Parameters for auth/JWT secrets
resource "aws_ssm_parameter" "jwt_secret" {
  name        = "/${var.app_name}/${var.environment}/jwt-secret"
  description = "JWT signing secret for ${var.app_name}"
  type        = "SecureString"
  value       = var.jwt_secret

  tags = {
    Environment = var.environment
  }
}

resource "aws_ssm_parameter" "auth_client_id" {
  name        = "/${var.app_name}/${var.environment}/auth-client-id"
  description = "API auth client ID for ${var.app_name}"
  type        = "String"
  value       = var.auth_client_id

  tags = {
    Environment = var.environment
  }
}

resource "aws_ssm_parameter" "auth_client_secret" {
  name        = "/${var.app_name}/${var.environment}/auth-client-secret"
  description = "API auth client secret for ${var.app_name}"
  type        = "SecureString"
  value       = var.auth_client_secret

  tags = {
    Environment = var.environment
  }
}

# ECS Cluster + Service
module "ecs" {
  source = "./modules/ecs"

  app_name                    = var.app_name
  environment                 = var.environment
  vpc_id                      = module.vpc.vpc_id
  public_subnet_ids           = module.vpc.public_subnet_ids
  private_subnet_ids          = module.vpc.private_subnet_ids
  alb_security_group_id       = module.vpc.alb_security_group_id
  ecs_tasks_security_group_id = module.vpc.ecs_tasks_security_group_id
  ssm_prefix                  = "/${var.app_name}/${var.environment}"

  depends_on = [
    module.rds,
    module.elasticache,
    module.amazonmq,
    aws_ssm_parameter.jwt_secret,
    aws_ssm_parameter.auth_client_id,
    aws_ssm_parameter.auth_client_secret,
  ]
}

# Outputs
output "vpc_id" {
  value = module.vpc.vpc_id
}

output "ecs_cluster_name" {
  value = module.ecs.cluster_name
}

output "ecs_service_name" {
  value = module.ecs.service_name
}

output "ecr_repository_url" {
  value = module.ecs.ecr_repository_url
}

output "ecr_repository_name" {
  value = module.ecs.ecr_repository_name
}

output "alb_dns_name" {
  value       = module.ecs.alb_dns_name
  description = "The DNS name of the ALB — use this to access the application"
}

output "rds_endpoint" {
  value     = module.rds.endpoint
  sensitive = true
}

output "db_name" {
  value = module.rds.db_name
}

output "redis_host" {
  value     = module.elasticache.redis_host
  sensitive = true
}

output "mq_endpoint" {
  value     = module.amazonmq.broker_endpoint
  sensitive = true
}
