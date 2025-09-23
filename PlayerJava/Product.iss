; =========================================================
; Script de Instalação PlayerJava
; =========================================================

[Setup]
AppName=PlayerJava
AppVersion=1.0
DefaultDirName={pf}\PlayerJava
DefaultGroupName=PlayerJava
UninstallDisplayIcon={app}\PlayerJava.exe
OutputDir=G:\Nova pasta\PlayerJava\Output
OutputBaseFilename=PlayerJavaSetup
Compression=lzma
SolidCompression=yes
WizardStyle=modern
AllowNoIcons=yes
DisableProgramGroupPage=no
DisableWelcomePage=no
DisableFinishedPage=no

[Languages]
Name: "portuguese"; MessagesFile: "C:\Program Files (x86)\Inno Setup 6\Languages\Portuguese.isl"

[Files]
Source: "G:\Nova pasta\PlayerJava\Output\PlayerJava.exe"; DestDir: "{app}"; Flags: ignoreversion
Source: "G:\Nova pasta\PlayerJava\LISTA_IPTV.TXT"; DestDir: "{app}"; Flags: ignoreversion

[Icons]
Name: "{group}\PlayerJava"; Filename: "{app}\PlayerJava.exe"; IconFilename: "G:\Nova pasta\PlayerJava\icon\ícone-para-app-de-IP.ico"
Name: "{userdesktop}\PlayerJava"; Filename: "{app}\PlayerJava.exe"; IconFilename: "G:\Nova pasta\PlayerJava\icon\ícone-para-app-de-IP.ico"

[Run]
Filename: "{app}\PlayerJava.exe"; Description: "Executar PlayerJava"; Flags: nowait postinstall skipifsilent
