const csvFixtures = {
  ucsbEgrades: `Enrl Cd,Perm #,Grade,Final Units,Student Last,Student First Middle,Quarter,Course ID,Section,Meeting Time(s) / Location(s),Email,ClassLevel,Major1,Major2,Date/Time,Pronoun
08235,A123456,,4.0,GAUCHO,CHRIS FAKE,F23,CMPSC156,0100,T R   2:00- 3:15 SH 1431     W    5:00- 5:50 PHELP 3525  W    6:00- 6:50 PHELP 3525  W    7:00- 7:50 PHELP 3525  ,cgaucho@ucsb.edu,SR,CMPSC,,9/27/2023 9:39:25 AM,
08250,A987654,,4.0,DEL PLAYA,LAUREN,F23,CMPSC156,0100,T R   2:00- 3:15 SH 1431     W    5:00- 5:50 PHELP 3525  W    6:00- 6:50 PHELP 3525  W    7:00- 7:50 PHELP 3525  ,ldelplaya@umail.ucsb.edu,SR,CMPSC,,9/27/2023 9:39:25 AM,She (She/Her/Hers)
08243,1234567,,4.0,TARDE,SABADO,F23,CMPSC156,0100,T R   2:00- 3:15 SH 1431     W    5:00- 5:50 PHELP 3525  W    6:00- 6:50 PHELP 3525  W    7:00- 7:50 PHELP 3525  ,sabadotarde@umail.ucsb.edu,SR,CMPSC,,9/27/2023 9:39:25 AM,He (He/Him/His)`,
  chicoStateCanvas: `Student Name,Student ID,Student SIS ID,Email,Section Name
Marge Simpson,88200,013228559,msimpson@csuchico.edu,CSED 500 - 362 Computational Thinking Summer 2025
Homer Simpson,88001,013205354,hsimpson@csuchico.edu,CSED 500 - 362 Computational Thinking Summer 2025
Ralph Wiggum,88003,013251642,rwiggum@csuchico.edu,CSED 500 - 362 Computational Thinking Summer 2025`,
  rosterDownload: `"COURSEID","EMAIL","FIRSTNAME","GITHUBID","GITHUBLOGIN","ID","LASTNAME","ORGSTATUS","ROSTERSTATUS","SECTION","STUDENTID","TEAMS","USERID"
"156","cgaucho@ucsb.edu","CHRIS","12345678","cgaucho","1","GAUCHO","MEMBER","ROSTER","0100","A123456","Team Alpha","1001"
"156","ldelplaya@ucsb.edu","LAUREN","87654321","ldelplaya","2","DEL PLAYA","MEMBER","ROSTER","0100","A987654","Team Beta","1002"
"156","sabadotarde@ucsb.edu","SABADO","","","3","TARDE","INVITED","MANUAL","","1234567","","0"`,
  oregonStateCSV: `Full name,Sortable name,Canvas user id,Overall course grade,Assignment on time percent,Last page view time,Last participation time,Last logged out,Email,SIS Id
Tom Smith,"Smith, Tom",6056208,96.25,80.4,2-Jul-25,11-Jun-25,21-May-25,tomsmith@oregonstate.edu,931551625
Martha Washington,"Washington, Martha",9876543,100,100,8-Aug-25,12-Dec-25,5-May-25,martha@oregonstate.edu,123456789
John Doe,"Doe, John",1234567,88.5,75.0,15-Jul-25,10-Jun-25,5-May-25,johndoe@oregonstate.edu,987654321
Alice Johnson,"Johnson, Alice",7654321,92.0,85.5,20-Jun-25,18-Jun-25,10-Jun-25,alicejohnson@oregonstate.edu,192837465
Bob Lee,"Lee, Bob",2468135,78.75,60.0,5-May-25,2-May-25,1-May-25,boblee@oregonstate.edu,564738291`,
  teamsByEmail: `team,email
team1,bob@ucsb.edu
team2,ldelplaya@ucsb.edu
team2,cgaucho@ucsb.edu
team3,sabadotarde@ucsb.edu`,
};

export default csvFixtures;
