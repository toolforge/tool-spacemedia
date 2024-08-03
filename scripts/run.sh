#!/bin/sh

DIR='/data/project/spacemedia'

# Load secrets and configuration
. $DIR/env.sh

resetDuplicates=false
resetIgnored=false
resetPerceptualHashes=false
resetSha1Hashes=false
resetProblems=false
updateFullresImages=true
searchEnabled=false

version=`cat $DIR/spacemedia/version.txt`

apptype=$1
profile=$2

export PATH="$DIR:$DIR/libwebp-1.2.4-linux-x86-64/bin:/usr/local/bin:$PATH"

#if [ "$apptype" = "youtube" ] ; then
#  youtube-dl -U
#fi

if [ "$apptype" = "web" ] ; then
#  nthreads=2
  JAR="$DIR/spacemedia/target/${apptype}/spacemedia-${version}.jar"
  LOG="$DIR/conf/logback-spring-toolforge.xml"
  SPRING_PROFILES="cloudvps-starthashes"
  MODE="LOCAL"
else
#  nthreads=1
  JAR="$DIR/spacemedia/target/job-${apptype}/spacemedia-${version}.jar"
  LOG=$DIR/conf/logback-spring-${profile}.xml
  if [ ! -f $LOG ] ; then
    cp $DIR/spacemedia/target/classes/logback-spring-toolforge.xml $LOG
    sed -i "s/spacemedia\\.log/${profile}.log/g" $LOG
  fi
  SPRING_PROFILES="job-${profile}"
  MODE="LOCAL"
fi

dupes=190
dpla=50

killall -q java

rm -f ~/files/*
rm -f /tmp/spacemedia*
rm -f /tmp/sm*
rm -f /tmp*.mp4
rm -Rf /tmp/tomcat*
rm -f /tmp/MediaDataBox*

if [ -d /srv/tmp ] ; then
  rm -f /srv/tmp/*
  TMPDIR=/srv/tmp
else
  TMPDIR=/tmp
fi

java -Xms$MEM -Xmx$MEM \
--add-opens java.base/java.lang=ALL-UNNAMED \
-Dexecution.mode=$MODE \
-Djava.io.tmpdir=$TMPDIR \
-Dsentry.dsn=$SENTRY_DSN \
-Dsentry.stacktrace.app.packages=org.wikimedia.commons.donvip.spacemedia \
-Dsentry.stacktrace.hidecommon=False \
-Dspring.security.oauth2.client.provider.wikimedia.authorization-uri=https://meta.wikimedia.org/w/rest.php/oauth2/authorize \
-Dspring.security.oauth2.client.provider.wikimedia.token-uri=https://meta.wikimedia.org/w/rest.php/oauth2/access_token \
-Dspring.security.oauth2.client.provider.wikimedia.user-info-uri=https://meta.wikimedia.org/w/rest.php/oauth2/resource/profile \
-Dspring.security.oauth2.client.provider.wikimedia.user-info-authentication-method=header \
-Dspring.security.oauth2.client.provider.wikimedia.userNameAttribute=username \
-Dspring.security.oauth2.client.registration.wikimedia.authorizationGrantType=authorization_code \
-Dspring.security.oauth2.client.registration.wikimedia.redirectUri={baseUrl}/{action}/oauth2/code/{registrationId} \
-Dspring.security.oauth2.client.registration.wikimedia.client-id=$OAUTH2_WIKIMEDIA_CLIENT_ID \
-Dspring.security.oauth2.client.registration.wikimedia.client-secret=$OAUTH2_WIKIMEDIA_CLIENT_SECRET \
-Dspring.security.oauth2.client.registration.wikimedia.client-authentication-method=client_secret_post \
-Ddomain.datasource.url=jdbc:mariadb://tools.db.svc.wikimedia.cloud:3306/${user}__spacemedia \
-Ddomain.datasource.username=$user \
-Ddomain.datasource.password=$password \
-Dcommons.datasource.url=jdbc:mariadb://commonswiki.analytics.db.svc.wikimedia.cloud:3306/commonswiki_p \
-Dhashes.datasource.url=jdbc:mariadb://$TROVE_INSTANCE.svc.trove.eqiad1.wikimedia.cloud:3306/hash_associations \
-Dcommons.datasource.username=$user \
-Dcommons.datasource.password=$password \
-Dhashes.datasource.username=$hashes_user \
-Dhashes.datasource.password=$hashes_password \
-Dflickr.api.key=$FLICKR_API_KEY \
-Dflickr.secret=$FLICKR_SECRET \
-Dcommons.api.password=$COMMONS_API_PASSWORD \
-Dcommons.api.oauth1.consumer-token=$COMMONS_API_OAUTH1_CONSUMER_TOKEN \
-Dcommons.api.oauth1.consumer-secret=$COMMONS_API_OAUTH1_CONSUMER_SECRET \
-Dcommons.api.oauth1.access-token=$COMMONS_API_OAUTH1_ACCESS_TOKEN \
-Dcommons.api.oauth1.access-secret=$COMMONS_API_OAUTH1_ACCESS_SECRET \
-Ddvids.api.key=$DVIDS_API_KEY \
-Dyoutube.api.key=$YOUTUBE_API_KEY \
-Dgoogle.translate.project=$GOOGLE_TRANSLATE_PROJECT \
-Dgoogle.translate.privateKeyId=$GOOGLE_TRANSLATE_PRIVATE_KEY_ID \
-Dgoogle.translate.privateKey="$GOOGLE_TRANSLATE_PRIVATE_KEY" \
-Dgoogle.translate.clientEmail=$GOOGLE_TRANSLATE_CLIENT_EMAIL \
-Dgoogle.translate.clientId=$GOOGLE_TRANSLATE_CLIENT_ID \
-Dtwitter.api.oauth1.consumer-token=$TWITTER_API_OAUTH1_CONSUMER_TOKEN \
-Dtwitter.api.oauth1.consumer-secret=$TWITTER_API_OAUTH1_CONSUMER_SECRET \
-Dtwitter.api.oauth1.access-token=$TWITTER_API_OAUTH1_ACCESS_TOKEN \
-Dtwitter.api.oauth1.access-secret=$TWITTER_API_OAUTH1_ACCESS_SECRET \
-Dmastodon.api.oauth2.access-token=$MASTODON_API_OAUTH2_ACCESS_TOKEN \
-Dmastodon.api.oauth2.client-id=$MASTODON_API_OAUTH2_CLIENT_ID \
-Dmastodon.api.oauth2.client-secret=$MASTODON_API_OAUTH2_CLIENT_SECRET \
-Dbox.api.oauth2.client-id=$BOX_API_OAUTH2_CLIENT_ID \
-Dbox.api.oauth2.client-secret=$BOX_API_OAUTH2_CLIENT_SECRET \
-Dbox.api.user-email=$BOX_API_USER_EMAIL \
-Dbox.api.user-password=$BOX_API_USER_PASSWORD \
-Dsearch.enabled=$searchEnabled \
-Dreset.duplicates=$resetDuplicates -Dreset.perceptual.hashes=$resetPerceptualHashes -Dreset.sha1.hashes=$resetSha1Hashes -Dreset.problems=$resetProblems -Dreset.ignored=$resetIgnored \
-Dupdate.fullres.images=$updateFullresImages \
-Dcommons.duplicates.max.files=$dupes \
-Dcommons.dpla.max.duplicates=$dpla \
-Dlogging.config=$LOG \
-Dspring.jpa.show-sql=false \
-Dspring.jpa.properties.hibernate.format_sql=false \
-jar $JAR --spring.profiles.active=$SPRING_PROFILES \
$3

