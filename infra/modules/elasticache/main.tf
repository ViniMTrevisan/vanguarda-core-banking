# ElastiCache (Redis) Module

variable "app_name" {
  type = string
}

variable "environment" {
  type = string
}

variable "subnet_ids" {
  type        = list(string)
  description = "Private subnet IDs for ElastiCache"
}

variable "security_group_id" {
  type        = string
  description = "Security group ID for ElastiCache"
}

# ElastiCache Subnet Group
resource "aws_elasticache_subnet_group" "main" {
  name       = "${var.app_name}-${var.environment}-redis"
  subnet_ids = var.subnet_ids

  tags = {
    Name        = "${var.app_name}-${var.environment}-redis"
    Environment = var.environment
  }
}

# Random auth token (must be 16-128 chars, alphanumeric only)
resource "random_password" "redis_auth_token" {
  length  = 32
  special = false
}

# ElastiCache Redis Replication Group (single node, auth + TLS)
resource "aws_elasticache_replication_group" "redis" {
  replication_group_id = "${var.app_name}-${var.environment}"
  description          = "Redis for ${var.app_name} ${var.environment}"

  node_type            = "cache.t3.micro"
  num_cache_clusters   = 1
  parameter_group_name = "default.redis7"
  engine_version       = "7.1"
  port                 = 6379

  subnet_group_name  = aws_elasticache_subnet_group.main.name
  security_group_ids = [var.security_group_id]

  # Enable auth token + TLS for secure access
  auth_token                 = random_password.redis_auth_token.result
  transit_encryption_enabled = true
  at_rest_encryption_enabled = true

  apply_immediately = true

  tags = {
    Name        = "${var.app_name}-${var.environment}-redis"
    Environment = var.environment
  }
}

# Store Redis host in SSM
resource "aws_ssm_parameter" "redis_host" {
  name        = "/${var.app_name}/${var.environment}/redis-host"
  description = "ElastiCache Redis host for ${var.app_name}"
  type        = "String"
  value       = aws_elasticache_replication_group.redis.primary_endpoint_address

  tags = {
    Environment = var.environment
  }
}

# Store Redis auth token (password) in SSM
resource "aws_ssm_parameter" "redis_password" {
  name        = "/${var.app_name}/${var.environment}/redis-password"
  description = "ElastiCache Redis auth token for ${var.app_name}"
  type        = "SecureString"
  value       = random_password.redis_auth_token.result

  tags = {
    Environment = var.environment
  }
}

# Outputs
output "redis_host" {
  value = aws_elasticache_replication_group.redis.primary_endpoint_address
}

output "redis_port" {
  value = aws_elasticache_replication_group.redis.port
}
