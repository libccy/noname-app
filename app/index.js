'use strict';

if (!localStorage.getItem('noname_inited')) {
	class App {
		initialize() {
			this.bindEvents();
		}
		bindEvents() {
			if (window.require && window.__dirname) this.onDeviceReady();
			else {
				const script = document.createElement('script');
				script.src = 'cordova.js';
				document.head.appendChild(script);
				document.addEventListener('deviceready', this.onDeviceReady, false);
			}
		}
		onDeviceReady() {
			const SITE_FASTGIT = 'https://raw.fgit.cf/libccy/noname/';
			const SITE_GITCODE = 'https://gitcode.net/sinat_33405273/noname/-/raw/';
			const SITE_GITHUB = 'https://raw.githubusercontent.com/libccy/noname/';
			let site = SITE_GITCODE;
			const button = document.createElement('div');
			button.id = 'button';
			const touchStart = function () {
				if (!this.classList.contains('disabled')) this.style.transform = 'scale(0.98)';
			};
			const touchEnd = function () {
				this.style.transform = '';
			};
			button.ontouchstart = touchStart;
			button.ontouchend = touchEnd;
			button.onmousedown = touchStart;
			button.onmouseup = touchEnd;
			button.onmouseleave = touchEnd;
			document.body.appendChild(button);
			const help = document.createElement('div');
			let version;
			const versionNode = document.createElement('div');
			versionNode.id = 'version';
			document.body.appendChild(versionNode);
			const req = async (url, onLoad, onError, target) => {
				if (!onLoad) return;

				try {
					eval(await (await fetch(url, {
						referrerPolicy: 'no-referrer'
					})).text());

					if (target) {
						if (!window[target]) throw new ReferenceError();

						await onLoad();

						delete window[target];
					} else await onLoad();
				} catch {
					if (onError) await onError();
				}
			}

			const checkConnection = () => {
				button.textContent = '正在连接';
				button.classList.add('disabled');
				versionNode.textContent = '';
				req(`${site}master/game/update.js`, () => {
					button.classList.remove('disabled');
					button.textContent = '下载无名杀';
					version = window.noname_update.version;
					versionNode.innerHTML = `v${version}`;
				}, () => {
					button.classList.add('disabled');
					button.textContent = '连接失败';
				}, 'noname_update');
			};

			let dir;
			const ua = navigator.userAgent.toLowerCase();

			if (ua.includes('android')) dir = cordova.file.externalApplicationStorageDirectory;
			else if (ua.includes('iphone') || ua.includes('ipad')) dir = cordova.file.documentsDirectory;

			const update = () => {
				button.textContent = '正在连接';
				button.classList.add('disabled');
				versionNode.textContent = '';
				req(`${site}v${version}/game/source.js`, () => {
					button.remove();
					help.remove();
					versionNode.remove();

					const prompt = document.createElement('div');
					prompt.style.height = '40px';
					prompt.style.top = 'calc(50% - 40px)';
					prompt.style.lineHeight = '40px';
					prompt.textContent = '正在下载游戏文件';
					document.body.appendChild(prompt);

					const progress = document.createElement('div');
					progress.style.top = 'calc(50% + 20px)';
					progress.style.fontSize = '20px';
					progress.textContent = '0/0';
					document.body.appendChild(progress);

					const updates = window.noname_source_list;
					delete window.noname_source_list;

					let n1 = 0;
					const n2 = updates.length;
					progress.textContent = `${n1}/${n2}`;
					const finish = () => {
						prompt.textContent = '游戏文件下载完毕';
						progress.textContent = `${n1}/${n2}`;

						if (window.FileTransfer) localStorage.setItem('noname_inited', dir);
						else localStorage.setItem('noname_inited', 'nodejs');

						setTimeout(() => window.location.reload(), 1000);
					}
					let downloadFile;
					if (window.FileTransfer) downloadFile = (url, folder, onSuccess, onError) => {
						const fileTransfer = new FileTransfer();
						url = `${site}v${version}/${url}`;
						folder = `${dir}${folder}`;
						console.log(url);
						fileTransfer.download(encodeURI(url), folder, onSuccess, onError);
					};
					else {
						const {
							mkdir,
							writeFileSync,
							access
						} = require('fs');
						downloadFile = (url, folder, onSuccess, onError) => {
							url = `${site}v${version}/${url}`;
							const dir = folder.split('/');
							let str = '';
							const download = async () => {
								try {
									writeFileSync(`${__dirname}/${folder}`, Buffer.from(await (await fetch(encodeURI(url), {
										referrerPolicy: 'no-referrer'
									})).arrayBuffer()));

									if (onSuccess) await onSuccess();
								} catch {
									if (onError) await onError();
								}
							}
							const accessRecursively = () => {
								if (dir.length <= 1) {
									download();
									return;
								}

								str += `/${dir.shift()}`;
								access(`${__dirname}${str}`, errnoException => {
									if (errnoException) try {
										mkdir(`${__dirname}${str}`, accessRecursively);
									} catch (e) {
										onError();
									} else accessRecursively();
								});
							}
							accessRecursively();
						};
					}

					updates.forEach(current => {
						const onSuccessOrFinish = () => {
							progress.textContent = `${++n1}/${n2}`

							if (n1 >= n2) setTimeout(finish, 500);
						};
						const downloadFileWithRetrying = () => downloadFile(current, current, onSuccessOrFinish, downloadFileWithRetrying);
						downloadFileWithRetrying();
					});
				}, () => {
					button.classList.add('disabled');
					button.textContent = '连接失败';
				}, 'noname_source_list');
			}

			const link = document.createElement('link');
			link.rel = 'stylesheet';
			link.href = 'app/index.css';
			document.head.appendChild(link);

			button.onclick = function () {
				if (!this.classList.contains('disabled')) update();
			};
			document.ontouchmove = touchEvent => touchEvent.preventDefault();

			const changeSite = document.createElement('div');
			changeSite.classList.add('blue-text');
			changeSite.id = 'change-site';
			changeSite.textContent = '下载源: GitCode';
			document.body.appendChild(changeSite);

			help.id = 'help';
			help.textContent = '无法在线下载？';
			const helpNode = document.createElement('div');
			helpNode.id = 'noname-init-help';
			const helpNodeText = document.createElement('div');
			const contentDivision = document.createElement('div');
			const orderedList = document.createElement('ol');
			const firstListItem = document.createElement('li');
			const latestAnchor = document.createElement('a');
			latestAnchor.append(latestAnchor.href = 'https://github.com/libccy/noname/releases/latest');
			firstListItem.append('访问', latestAnchor, '，下载zip文件');
			const secondListItem = document.createElement('li');
			secondListItem.append(
				'解压后将noname-master目录内的所有文件放入对应文件夹：',
				document.createElement('br'),
				'windows/linux：resources/app',
				document.createElement('br'),
				'mac：（右键显示包内容）contents/resources/app',
				document.createElement('br'),
				'android：android/data/com.widget.noname',
				document.createElement('br'),
				'ios：documents（itunes—应用—文件共享）'
			);
			const thirdListItem = document.createElement('li');
			const reloadAnchor = document.createElement('a');
			reloadAnchor.href = `javascript:localStorage.setItem('noname_inited',window.tempSetNoname);window.location.reload();`;
			reloadAnchor.append('点击此处');
			thirdListItem.append('完成上述步骤后，', reloadAnchor);
			orderedList.append(firstListItem, secondListItem, thirdListItem);
			contentDivision.append(orderedList);
			helpNodeText.append(contentDivision);
			helpNode.appendChild(helpNodeText);
			help.onclick = () => {
				document.body.appendChild(helpNode);
			};

			const back = document.createElement('div');
			back.id = 'back';
			back.textContent = '返回';
			back.onclick = () => {
				helpNode.remove();
			};
			helpNode.appendChild(back);
			document.body.appendChild(help);
			checkConnection();

			if (window.FileTransfer) window.tempSetNoname = dir;
			else window.tempSetNoname = 'nodejs';

			changeSite.onclick = function () {
				switch (site) {
					case SITE_GITCODE:
						site = SITE_FASTGIT;
						this.textContent = '下载源: FastGit'
						break;
					case SITE_FASTGIT:
						site = SITE_GITHUB;
						this.textContent = '下载源: GitHub'
						break;
					default:
						site = SITE_GITCODE;
						this.textContent = '下载源: GitCode'
				}

				checkConnection();
			};

			const importData = document.createElement('div');
			importData.id = 'import-data';
			importData.innerHTML = '导入数据文件';
			document.body.appendChild(importData);

			const importSelect = document.createElement('select');
			importSelect.id = 'import-select';
			document.body.appendChild(importSelect);

			const game = {
				putDB(storeName, idbValidKey, value, onSuccess, onError) {
					return lib.db ? new Promise((resolve, reject) => {
						const record = lib.db.transaction([storeName], 'readwrite').objectStore(storeName).put(value, idbValidKey);
						record.onerror = event => {
							if (typeof onError == 'function') {
								onError(event);
								resolve(null);
							} else reject(event);
						};
						record.onsuccess = event => {
							if (typeof onSuccess == 'function') onSuccess(event);

							resolve(event);
						};
					}) : Promise.resolve(value);
				}
			};
			const lib = {
				configprefix: 'noname_0.9_',
				init: {
					decode(str) {
						return atob(str).replace(
							/[\u00e0-\u00ef][\u0080-\u00bf][\u0080-\u00bf]/g,
							substring => String.fromCharCode(((substring.charCodeAt(0) & 0x0f) << 12) | ((substring.charCodeAt(1) & 0x3f) << 6) | (substring.charCodeAt(2) & 0x3f))
						).replace(
							/[\u00c0-\u00df][\u0080-\u00bf]/g,
							substring => String.fromCharCode((substring.charCodeAt(0) & 0x1f) << 6 | substring.charCodeAt(1) & 0x3f)
						);
					}
				}
			};

			if (typeof __dirname === 'string' && __dirname.length) {
				__dirname.split('/').forEach(substring => {
					if (!substring) return;

					const character = substring[0];
					lib.configprefix += /[A-Z]|[a-z]/.test(character) ? character : '_';
				});
				lib.configprefix += '_';
			}

			const index = window.location.href.indexOf('index.html?server=');

			if (index != -1) {
				window.isNonameServer = window.location.href.slice(index + 18);
				window.nodb = true;
			}

			if (localStorage.getItem(`${lib.configprefix}nodb`)) window.nodb = true;

			if (window.FileTransfer) {
				window.tempSetNoname = dir;

				game.getFileList = (_directory, success, failure) => {
					var files = [], folders = [];
					window.resolveLocalFileSystemURL(dir + _directory, entry => {
						var dirReader = entry.createReader();
						var entries = [];
						var readEntries = () => {
							dirReader.readEntries(results => {
								if (!results.length) {
									entries.sort();
									for (var i = 0; i < entries.length; i++) {
										if (entries[i].isDirectory) {
											folders.push(entries[i].name);
										}
										else {
											files.push(entries[i].name);
										}
									}
									success(folders, files);
								}
								else {
									entries = entries.concat(Array.from(results));
									readEntries();
								}
							}, failure);
						};
						readEntries();
					}, failure);
				};

				game.readFileAsText = (fileName, callback, onError) => window.resolveLocalFileSystemURL(dir, entry => entry.getFile(fileName, {}, fileEntry => fileEntry.file(fileToLoad => {
					const fileReader = new FileReader();
					fileReader.onload = progressEvent => callback(progressEvent.target.result);
					fileReader.readAsText(fileToLoad, "UTF-8");
				}, onError), onError), onError);
			} else {
				window.tempSetNoname = 'nodejs';

				game.getFileList = (directory, success, failure) => {
					const files = [];
					const folders = [];
					directory = `${__dirname}/${directory}`;

					if (typeof failure == "undefined") failure = error => {
						throw error;
					};
					else if (failure == null) failure = () => void 0;

					try {
						require('fs').readdir(directory, (err, fileList) => {
							if (err) {
								failure(err);
								return;
							}
							fileList.forEach(file => {
								if (file[0] == '.' || file[0] == '_') return;

								if (require('fs').statSync(`${directory}/${file}`).isDirectory()) folders.push(file);
								else files.push(file);
							});
							success(folders, files);
						});
					} catch (error) {
						failure(error);
					}
				};

				game.readFileAsText = (fileName, callback, onError) => require('fs').readFile(`${__dirname}/${fileName}`, 'utf-8', (errnoException, data) => {
					if (errnoException) onError(errnoException);
					else callback(data);
				});
			}

			const addOption = name => {
				const option = document.createElement('option');
				option.value = name;
				option.innerHTML = name;
				importSelect.appendChild(option);
				return option;
			}

			addOption('默认');
			addOption('选择系统文件');

			game.getFileList('files/', (_, files) => files.filter(fileName => fileName.startsWith('无名杀 - 数据 -')).forEach(addOption), error => console.error('读取无名杀数据文件失败:', error));

			importSelect.onchange = () => {
				if (importSelect.value == '默认') return;
				if (confirm(`是否从“${importSelect.value}”导入无名杀数据？`)) {
					new Promise((resolve, reject) => {
						if (lib.db || window.nodb) return resolve(null);
						const idbOpenDBRequest = window.indexedDB.open(`${lib.configprefix}data`);
						idbOpenDBRequest.onerror = reject;
						idbOpenDBRequest.onsuccess = resolve;
						idbOpenDBRequest.onupgradeneeded = idbVersionChangeEvent => {
							const idbDatabase = idbVersionChangeEvent.target.result;
							if (!idbDatabase.objectStoreNames.contains('video')) idbDatabase.createObjectStore('video', {
								keyPath: 'time'
							});
							if (!idbDatabase.objectStoreNames.contains('image')) idbDatabase.createObjectStore('image');
							if (!idbDatabase.objectStoreNames.contains('audio')) idbDatabase.createObjectStore('audio');
							if (!idbDatabase.objectStoreNames.contains('config')) idbDatabase.createObjectStore('config');
							if (!idbDatabase.objectStoreNames.contains('data')) idbDatabase.createObjectStore('data');
						};
					}).then(event => {
						if (!lib.db && !window.nodb) lib.db = event.target.result;
						return new Promise((resolve, reject) => {
							if (importSelect.value != '选择系统文件') {
								game.readFileAsText('files/' + importSelect.value, data => {
									if (!data) return reject('no data');
									try {
										data = JSON.parse(lib.init.decode(data));
										if (!data || typeof data != 'object') {
											throw ('err');
										}
										if (lib.db && (!data.config || !data.data)) {
											throw ('err');
										}
									}
									catch (e) {
										return reject(e);
									}
									return resolve(data);
								}, reject);
							} else {
								if (document.getElementById("fileNameInput")) {
									document.body.removeChild(document.getElementById("fileNameInput"));
								}
								var inputObj = document.createElement('input');
								inputObj.setAttribute('id', 'fileNameInput');
								inputObj.setAttribute('type', 'file');
								inputObj.setAttribute('name', 'fileNameInput');
								inputObj.setAttribute("style", 'visibility:hidden');
								document.body.appendChild(inputObj);
								inputObj.value;
								inputObj.click();
								inputObj.addEventListener('change', e => {
									if (!inputObj.files) return;
									var fileToLoad = inputObj.files[0];
									if (fileToLoad) {
										var fileReader = new FileReader();
										fileReader.onload = function (fileLoadedEvent) {
											var data = fileLoadedEvent.target.result;
											if (!data) return reject('no data');
											try {
												data = JSON.parse(lib.init.decode(data));
												if (!data || typeof data != 'object') {
													throw ('err');
												}
												if (lib.db && (!data.config || !data.data)) {
													throw ('err');
												}
											}
											catch (e) {
												return reject(e);
											}
											return resolve(data);
										};
										fileReader.readAsText(fileToLoad, "UTF-8");
									}
								});
							}
						});
					}).then(async data => {
						if (!lib.db) {
							var noname_inited = localStorage.getItem('noname_inited');
							var onlineKey = localStorage.getItem(lib.configprefix + 'key');
							localStorage.clear();
							if (noname_inited) {
								localStorage.setItem('noname_inited', noname_inited);
							}
							if (onlineKey) {
								localStorage.setItem(lib.configprefix + 'key', onlineKey);
							}
							for (var i in data) {
								localStorage.setItem(i, data[i]);
							}
						}
						else {
							for (var i in data.config) {
								await game.putDB('config', i, data.config[i]);
							}
							for (var i in data.data) {
								await game.putDB('data', i, data.data[i]);
							}
						}
						localStorage.setItem('noname_inited', window.tempSetNoname);
						alert('导入成功');
						setTimeout(() => {
							window.location.reload();
						}, 1000);
					}).catch(error => {
						alert('导入失败: ' + error);
						console.error(error);
					})
				}
			};
		}
	}

	new App().initialize();
}
