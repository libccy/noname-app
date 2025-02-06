'use strict';

const {
	app,
	BrowserWindow
} = require('electron');

require("./noname-server.js");

let win;

function createWindow() {
	win = new BrowserWindow({
		width: 960,
		height: 660,
		title: '无名杀',
		icon: require('path').resolve(__dirname, 'noname.ico'),
		autoHideMenuBar: true,
		webPreferences: {
			nodeIntegration: true,
			nodeIntegrationInSubFrames: true,
			nodeIntegrationInWorker: true,
			contextIsolation: false,
			plugins: true,
			experimentalFeatures: true,
			webSecurity: false,
		}
	});
	win.maximize();
	win.loadURL("http://localhost:8089/index.html");
	win.on('closed', () => win = null);
}

app.on('ready', createWindow);
app.on('window-all-closed', () => app.quit());

app.on('activate', () => {
	if (win === null) createWindow();
});
