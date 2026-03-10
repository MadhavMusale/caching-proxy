#!/bin/bash
# ─────────────────────────────────────────────────────────────────────────────
# setup-ssl.sh — Automated Let's Encrypt SSL certificate setup
# Run this ONCE on your server after pointing your domain to your server's IP
#
# Usage: ./scripts/setup-ssl.sh your-domain.com your@email.com
# ─────────────────────────────────────────────────────────────────────────────
set -e

DOMAIN=${1:-""}
EMAIL=${2:-""}

if [ -z "$DOMAIN" ] || [ -z "$EMAIL" ]; then
    echo "Usage: $0 <domain> <email>"
    echo "Example: $0 proxy.example.com admin@example.com"
    exit 1
fi

echo "🔐 Setting up SSL for $DOMAIN"
echo ""

# Install certbot if not present
if ! command -v certbot &> /dev/null; then
    echo "📦 Installing certbot..."
    sudo apt-get update -q
    sudo apt-get install -y certbot python3-certbot-nginx
fi

# Update nginx config with actual domain
echo "⚙️  Updating nginx config with domain: $DOMAIN"
sed -i "s/YOUR_DOMAIN/$DOMAIN/g" nginx/nginx.conf

# Create webroot directory for challenge
mkdir -p /var/www/certbot

# Start nginx temporarily for the challenge
echo "🌐 Starting nginx for certificate challenge..."
docker compose up -d nginx 2>/dev/null || true

# Get certificate
echo "📜 Requesting certificate from Let's Encrypt..."
sudo certbot certonly \
    --webroot \
    --webroot-path=/var/www/certbot \
    --email "$EMAIL" \
    --agree-tos \
    --no-eff-email \
    -d "$DOMAIN" \
    -d "www.$DOMAIN"

# Reload nginx
echo "♻️  Reloading nginx with SSL..."
docker compose exec nginx nginx -s reload 2>/dev/null || docker compose restart nginx

# Set up auto-renewal cron
echo "⏰ Setting up auto-renewal..."
(crontab -l 2>/dev/null; echo "0 12 * * * /usr/bin/certbot renew --quiet && docker compose -f $(pwd)/docker-compose.yml exec nginx nginx -s reload") | crontab -

echo ""
echo "✅ SSL setup complete!"
echo "   https://$DOMAIN is now live and auto-renewing."
echo ""
echo "   Certificate location: /etc/letsencrypt/live/$DOMAIN/"
echo "   Auto-renewal: daily at 12:00 via cron"
