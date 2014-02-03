nfs4j: pure java implementation of NFSv3, v4 and V4.1 protocols.
================================================================

This is package contains __nfs4j-core__ library used by dCache NFS server. In addition to it there is __nfs4j-basic-server__ for testing and development.

HowTo build:
============
Then nfs4j uses maven for building and packaging:
```sh
$ mvn clean package
```
As a result you will get two packages built: core/target/nfs4j-core-0.X.X-SNAPSHOT.jar and basic-server/target/nfs4j-basic-server-0.X.X-SNAPSHOT.jar.

Basic server
============
For development and testing included a simple server. There are three files to configure the server behavior: chimera.properties, nfs.properties, exports.


exports
-------
The exports file supports almost all options supported by regular nfs servers:
```
/exports *(rw)
```
The supported options:

| option              |  description                                        |
|---------------------|-----------------------------------------------------|
| ro                  | read-only export                                    |
| rw                  | read-write exports                                  |
| root_squash         | map requests from uid/gid 0 to the anonymous uid/gid|
| no_root_squash      | turn off root squashing                             |
| all_squash          | map  all  uids  and  gids to the anonymous user     |
| acl                 | check NFSv4 ACL for this export                     |
| noacl               | do not check ACLs                                   |
| anonuid and anongid | set the uid and gid  of  the  anonymous account     |
| sec=<flavor>        | restricts the export to clients using this flavor   |


Support
=======
developers mailing list: __dev(-at-)dcache.org__
