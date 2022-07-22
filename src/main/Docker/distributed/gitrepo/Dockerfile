FROM gitea/gitea:1.16.5
EXPOSE 3000 22
RUN apk add mlocate emacs runuser
# RUN GITEA__security__INSTALL_LOCK=true runuser -u git -- /usr/local/bin/gitea -c /data/gitea/conf/app.ini admin user create --admin --username root -password manage --email noreply@softwareag.com
# RUN cat /data/gitea/conf/app.ini | sed -e s/3000/8081/ > /data/gitea/conf/app.ini.new
# RUN mv /data/gitea/conf/app.ini.new /data/gitea/conf/app.ini
