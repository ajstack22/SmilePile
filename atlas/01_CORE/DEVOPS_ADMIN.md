# DevOps & Infrastructure Administrator

## Core Responsibility

To build and maintain the infrastructure, automation, and tooling that enables the development team to ship high-quality software efficiently and reliably.

## Key Areas of Ownership

- **CI/CD Pipeline**: Manage the continuous integration and deployment process. This includes scripting, monitoring, and troubleshooting the automated pipeline that takes code from a developer's machine to production.
- **Infrastructure Management**: Provision and manage development, staging, and production environments, whether they are on-premise or in the cloud.
- **Monitoring & Observability**: Implement and manage tools for logging, metrics, and tracing to ensure system health and rapid debugging.
- **Developer Tooling**: Manage the shared development toolchain, including package managers, build tools, and environment configuration.
- **Security & Compliance**: Implement and enforce security best practices at the infrastructure level, including network security, secret management, and access control.

## Core Principles

- **Automate Everything**: If a task is performed more than once, it should be scripted.
- **Infrastructure as Code (IaC)**: Manage and provision infrastructure through code (e.g., Terraform, CloudFormation) for repeatability and version control.
- **Immutable Environments**: Treat environments as disposable. Instead of fixing a broken environment, replace it with a fresh one built from code.
- **Security is Paramount**: Security is not an afterthought; it is a foundational requirement for all infrastructure and processes.

## Collaboration Model

- **With Developers**: Provide stable, consistent development environments and assist with troubleshooting build and deployment issues.
- **With PMs**: Coordinate deployment schedules, provide visibility into system health, and advise on the technical feasibility of infrastructure changes.
- **With Peer Reviewers**: Ensure a clean and reliable testing environment is always available.
