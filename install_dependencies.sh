#!/bin/bash

# Install Anaconda
echo "Installing Anaconda..."
ANACONDA_INSTALLER="Anaconda3-latest-Linux-x86_64.sh"
ANACONDA_URL="https://repo.anaconda.com/archive/$ANACONDA_INSTALLER"

wget "$ANACONDA_URL" -O /tmp/"$ANACONDA_INSTALLER"
chmod +x /tmp/"$ANACONDA_INSTALLER"
sudo bash /tmp/"$ANACONDA_INSTALLER" -b -p /opt/anaconda3

echo 'export PATH="/opt/anaconda3/bin:$PATH"' >> ~/.bashrc
source ~/.bashrc

# Install Docker
echo "Installing Docker..."
sudo apt update
sudo apt install -y apt-transport-https ca-certificates curl software-properties-common
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg
echo "deb [arch=amd64 signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
sudo apt update
sudo apt install -y docker-ce docker-ce-cli containerd.io

# Start and enable Docker service
sudo systemctl start docker
sudo systemctl enable docker

# Install Docker Compose
echo "Installing Docker Compose..."
DOCKER_COMPOSE_VERSION=$(curl -s https://api.github.com/repos/docker/compose/releases/latest | grep "tag_name" | cut -d '"' -f 4)
sudo curl -L "https://github.com/docker/compose/releases/download/$DOCKER_COMPOSE_VERSION/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

echo "Anaconda, Docker, and Docker Compose have been installed successfully."
