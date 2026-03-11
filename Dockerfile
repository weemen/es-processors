# Use the official Redis base image
FROM redis:latest

# You can add custom configuration if needed
# COPY redis.conf /usr/local/etc/redis/redis.conf
# CMD [ "redis-server", "/usr/local/etc/redis/redis.conf" ]

# For now, just start the default redis-server
EXPOSE 6379
CMD ["redis-server"]
