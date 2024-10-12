#!/bin/bash
set -e

usage="$(basename "$0") [-h] [-n NAME] [-d DOMAIN] [-u EMAIL] [-b BUCKET]
Use AWS sam to build and deploy the PassHelper CloudFormation application. This action will deploy PassHelper to the AWS account configured in your local AWS profile.
    -h  show this help text
    -n  application name, e.g passhelper
    -d  domain name including TLD, a hosted zone must exist in AWS Route53, the application will be hosted at passports.{domain}
    -b  S3 bucket name used to store application resources
    -u  email address to use for the initial user, this will be the user you use to log in with and will be created in AWS Cognito"

options=':h:n:d:u:b:'
while getopts $options option; do
  case "$option" in
    h) echo "$usage"; exit;;
    n) NAME=$OPTARG;;
    d) DOMAIN=$OPTARG;;
    u) EMAIL=$OPTARG;;
    b) BUCKET=$OPTARG;;
    :) printf "missing argument for -%s\n" "$OPTARG" >&2; echo "$usage" >&2; exit 1;;
   \?) printf "illegal option: -%s\n" "$OPTARG" >&2; echo "$usage" >&2; exit 1;;
  esac
done

if [ ! "$DOMAIN" ] || [ ! "$NAME" ] || [ ! "$EMAIL" ] || [ ! "$BUCKET" ]; then
  echo "arguments -n, -d, -b and -u must be provided"
  echo "$usage" >&2; exit 1
fi

echo "
Deploying PassHelper
Application name:               ${NAME}
Storage bucket:                 ${BUCKET}
Default account email address:  ${EMAIL}
Application will be hosted at passports.${DOMAIN}
"

sam build --use-container --parallel --cached -t passhelper.cf.yaml
sam deploy --stack-name "${NAME}" --s3-bucket "${BUCKET}" --capabilities CAPABILITY_IAM CAPABILITY_AUTO_EXPAND --tags AppManagerCFNStackKey="${NAME}" billing="${NAME}" --parameter-overrides \
  DomainName="${DOMAIN}" \
  InitialUserEmailAddress="${EMAIL}"
