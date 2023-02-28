======================================================
Oracle Free Use Terms and Conditions (FUTC) License 
======================================================
https://www.oracle.com/downloads/licenses/oracle-free-license.html
===================================================================

ojdbc11-full.tar.gz - JDBC Thin Driver and Companion JARS
========================================================
This TAR archive (ojdbc11-full.tar.gz) contains the 21.9.0.0 release of the Oracle JDBC Thin driver(ojdbc11.jar), the Universal Connection Pool (ucp.jar) and other companion JARs grouped by category. 

(1) ojdbc11.jar (5189606 bytes) - 
(SHA1 Checksum: 879c0746524c41cf5a257ec5349e522027aefae3)
Oracle JDBC Driver compatible with JDK8, JDK11, JDK12, JDK13, JDK14, and JDK15.

(2) ucp11.jar (1803345 bytes) - (SHA1 Checksum: 05ddef94bc64503594852893a63a8cded8587ded)
Universal Connection Pool classes to be used with ojdbc11.jar -- for performance, scalability, high availability, sharded and multitenant databases.

(3) rsi.jar (345267 bytes) - (SHA1 Checksum: ff84ee867ccdea5bc92e6f0a334f513d08a0b1a4)
Reactive Streams Ingestion (RSI) 
(4) ojdbc.policy (21661 bytes) - Sample security policy file for Oracle Database JDBC drivers

======================
Security Related JARs
======================
Java applications require some additional jars to use Oracle Wallets. 
You need to use all the three jars while using Oracle Wallets. 

(5) oraclepki.jar (307822 bytes) - (SHA1 Checksum: 735ed4c309f7cbaaecb210ecb3ae2b42929c4acb)
Additional jar required to access Oracle Wallets from Java
(6) osdt_cert.jar (211063 bytes) - (SHA1 Checksum: 5c597f4249d01691388befb634a365c61764ba4e)
Additional jar required to access Oracle Wallets from Java
(7) osdt_core.jar (312983 bytes) - (SHA1 Checksum: 842052afe5b917ff40aee6bf19260456e97185d6)
Additional jar required to access Oracle Wallets from Java

=============================
JARs for NLS and XDK support 
=============================
(8) orai18n.jar (1664684 bytes) - (SHA1 Checksum: 2ed1bb11a31ad6c63dee2f7ee7fe06b62adc2085) 
Classes for NLS support
(9) xdb.jar (129570 bytes) - (SHA1 Checksum: ff5947f309aeeec5e2a9f89ef77987a45ae116a2)
Classes to support standard JDBC 4.x java.sql.SQLXML interface 
(10) xmlparserv2.jar (1953954 bytes) - (SHA1 Checksum: fe82614460c74db8a0cc60ec031bd2c8405fa083)
Classes to support standard JDBC 4.x java.sql.SQLXML interface 
(11) xmlparserv2_sans_jaxp_services.jar (1949184 bytes) - (SHA1 Checksum: 4553c886ff1b5a548bae32f76afb00847832a15c) 
Classes to support standard JDBC 4.x java.sql.SQLXML interface

====================================================
JARs for Real Application Clusters(RAC), ADG, or DG 
====================================================
(12) ons.jar (198688 bytes ) - (SHA1 Checksum: 982c64f9e2538dd92e42ff134a8d507a593929e4)
for use by the pure Java client-side Oracle Notification Services (ONS) daemon
(13) simplefan.jar (32432 bytes) - (SHA1 Checksum: f6e6424a8a5b38f528b18e1cc54911efb184229e)
Java APIs for subscribing to RAC events via ONS; simplefan policy and javadoc


==================================================================================
NOTE: The diagnosability JARs **SHOULD NOT** be used in the production environment. 
These JARs (ojdbc11_g.jar,ojdbc11dms.jar, ojdbc11dms_g.jar) are meant to be used in the 
development, testing, or pre-production environment to diagnose any JDBC related issues. 

=====================================
OJDBC - Diagnosability Related JARs
===================================== 

(14) ojdbc11_g.jar (8620615 bytes) - (SHA1 Checksum: 6b79a0e1ec317a0834a75690aade0fc1785a7ecf)
Same as ojdbc11.jar except compiled with "javac -g" and contains tracing code.

(15) ojdbc11dms.jar (7193367 bytes) - (SHA1 Checksum: fa44fb968dbb57bac19f404f2041ae9e9c7f30bd)
Same as ojdbc11.jar, except that it contains instrumentation to support DMS and limited java.util.logging calls.

(16) ojdbc11dms_g.jar (8622485 bytes) - (SHA1 Checksum: f36fa8bc1078f79935db97030cdc48e43bf56e7f)
Same as ojdbc11_g.jar except that it contains instrumentation to support DMS.

(17) dms.jar (2194533 bytes) - (SHA1 Checksum: f1a198d65d85eb4d54f8d83076f0ce05190e502f)
dms.jar required for DMS-enabled JAR files.

==================================================================
Oracle JDBC and UCP - Javadoc and README
==================================================================

(18) JDBC-Javadoc-21c.jar (2615975 bytes) - JDBC API Reference 21c

(19) ucp11-Javadoc-21c.jar (813218  bytes) - UCP Java API Reference 21c

(20) rsi-Javadoc-21c.jar (425964 bytes) - RSI Java API Reference 21c

(21) simplefan-Javadoc-21c.jar (32432 bytes) - Simplefan API Reference 21c 

(22) xdb-Javadoc-21c.jar (129570 bytes) - XDB API Reference 21c 

(23) xmlparserv2-Javadoc-21c.jar (1953954 bytes) - xmlparserv2 API Reference 21c 

(24) JDBC-Readme.txt: It contains general information about the JDBC driver and bugs that have been fixed in the 21.9.0.0 release. 

(25) UCP-Readme.txt: It contains general information about UCP and bugs that are fixed in the 21.9.0.0 release. 

=============== Known Problems in the Release 21c ==================== 

Refer to Bugs-fixed-in-21c.txt on JDBC download page (https://www.oracle.com/database/technologies/appdev/jdbc-downloads.html) for the list of bugs fixed in the 21c release.

=================
USAGE GUIDELINES
=================
Refer to the JDBC Developers Guide (https://docs.oracle.com/en/database/oracle/oracle-database/21/jjdbc/index.html) and Universal Connection Pool Developers Guide (https://docs.oracle.com/en/database/oracle/oracle-database/21/jjucp/index.html) for more details.
