# chef-aws
Spring boot application with CHEF and Amazon web services.


To use Chef Server Implementantion you need to configure these properties in application.properties

### CHEF SERVER CONFIGURATIONS
        chef.server.ip=
        chef.server.organization=
        chef.server.user=
        chef.server.password=
  

To use a SQS Implementation, you need to configure these properties in application.yml:

### AWS Credentials

        cloud.aws: 
          credentials: 
            accessKey: 
            instanceProfile: false
            secretKey: 
          region: 
            auto: false
            static: sa-east-1
          stack: 
            auto: false

------------------------------------------------------------------------------

### SQS Queue Configuration - endpoint and name
    These properties are responsible to configure SQS Queue's     
      address:
         queue: 
           name:
             send: 
             listener: 
           url:
             listener: 
             send: 
  

 #### To Run Application you need to run this command:
 
     .\gradlew bootRun
