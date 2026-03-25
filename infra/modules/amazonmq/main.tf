# Amazon MQ (RabbitMQ) Module

variable "app_name" {
  type = string
}

variable "environment" {
  type = string
}

variable "subnet_ids" {
  type        = list(string)
  description = "Private subnet IDs for Amazon MQ (uses first subnet for SINGLE_INSTANCE)"
}

variable "security_group_id" {
  type        = string
  description = "Security group ID for Amazon MQ"
}

variable "mq_username" {
  type        = string
  description = "RabbitMQ admin username"
  default     = "vcb"
}

# Random password for RabbitMQ
resource "random_password" "mq_password" {
  length  = 32
  special = false
}

# Amazon MQ RabbitMQ Broker (single instance, cheapest)
resource "aws_mq_broker" "rabbitmq" {
  broker_name = "${var.app_name}-${var.environment}"

  engine_type        = "RabbitMQ"
  engine_version     = "3.13"
  host_instance_type = "mq.t3.micro"
  deployment_mode    = "SINGLE_INSTANCE"

  subnet_ids         = [var.subnet_ids[0]]
  security_groups    = [var.security_group_id]
  publicly_accessible = false

  user {
    username = var.mq_username
    password = random_password.mq_password.result
  }

  logs {
    general = true
  }

  tags = {
    Name        = "${var.app_name}-${var.environment}-mq"
    Environment = var.environment
  }
}

locals {
  # Extract hostname from the AMQPS endpoint (format: amqps://hostname:5671)
  # aws_mq_broker returns instances[0].endpoints as list; pick the AMQPS one
  amqps_endpoint = [for e in aws_mq_broker.rabbitmq.instances[0].endpoints : e if startswith(e, "amqps://")][0]
  # Extract just the hostname (strip amqps:// prefix and :5671 suffix)
  mq_host = regex("amqps://([^:]+):", local.amqps_endpoint)[0]
}

# Store RabbitMQ host in SSM
resource "aws_ssm_parameter" "mq_host" {
  name        = "/${var.app_name}/${var.environment}/rabbitmq-host"
  description = "Amazon MQ RabbitMQ host for ${var.app_name}"
  type        = "String"
  value       = local.mq_host

  tags = {
    Environment = var.environment
  }
}

# Store RabbitMQ username in SSM
resource "aws_ssm_parameter" "mq_username" {
  name        = "/${var.app_name}/${var.environment}/rabbitmq-username"
  description = "Amazon MQ RabbitMQ username for ${var.app_name}"
  type        = "String"
  value       = var.mq_username

  tags = {
    Environment = var.environment
  }
}

# Store RabbitMQ password in SSM
resource "aws_ssm_parameter" "mq_password" {
  name        = "/${var.app_name}/${var.environment}/rabbitmq-password"
  description = "Amazon MQ RabbitMQ password for ${var.app_name}"
  type        = "SecureString"
  value       = random_password.mq_password.result

  tags = {
    Environment = var.environment
  }
}

# Outputs
output "broker_endpoint" {
  value = local.amqps_endpoint
}

output "mq_host" {
  value = local.mq_host
}
