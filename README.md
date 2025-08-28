# websphereapps

Zbiór aplikacji Web przygotowanych pod kątem wykorzystania ich za pośrednictwem Serwera Aplikacji IBM WebSphere.

 ## Aktualizacja aplikacji z linii komend
 
 Można zaktualizować aplikację z linii komend za pomocą `wsadmin.sh`. Załóżmy, że mamy zainstalowany serwer IBM WebSphere w lokalizacji `/opt/IBM/BAW/21.0.3`.
 
 ```bash
 export WAS_HOME=/opt/IBM/BAW/21.0.3
 cd ${WAS_HOME}/bin
./wsadmin.sh
# Podajemy login i hsało użytkownika deploy manager'a, tak jak do konsoli webowej
```

Załóżmy, następujące parametry:
* `defaultapplication` - nazwa aplikacji, która jest zainstalowana.
* `/opt/IBM/BAW/bawadmin/defaultapplication.war` - plik WAR z aplikacją.

Będąc zalogowanym do `wsadmin` wydajemy polecenia (domyślnie `wsadmin` obsługuje **Jacl**):

```jacl
$AdminApp update defaultapplication app {-operation update -contents /opt/IBM/BAW/bawadmin/defaultapplication.war -usedefaultbindings -nodeployejb}
$AdminConfig save
$AdminNodeManagement syncActiveNodes
```

Źródło: [Updating installed applications using the wsadmin scripting tool](https://www.ibm.com/docs/en/was/8.5.5?topic=scripting-updating-installed-applications-using-wsadmin-tool).