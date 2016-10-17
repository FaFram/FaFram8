# FaFram8 EC2 client

### Known limitations

There seems to be a problem, when you try to use the jbossqe-fuse project as the parent of any project, which uses the EC2 Provision provider and thus also this client.

The main issue is the recursive import of camel-parent pom file and with it also the google.guice project, which causes problems whit classloading. I haven't found any easy solution for this problem for now. The only solution I have found so far is to not use the jbossqe-fuse as the parent, which of course causes can cause dependencies issues.

For more information about the uses of this client, contact person: tplevko@redhat.com
