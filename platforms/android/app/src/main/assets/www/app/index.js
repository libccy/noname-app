'use strict';
(function () {
    if (localStorage.getItem('noname_inited')) return;
    var app = {
        initialize: function () {
            this.bindEvents();
        },
        bindEvents: function () {
            if (window.require && window.__dirname) {
                this.onDeviceReady();
            }
            else {
                var script = document.createElement('script');
                script.src = 'cordova.js';
                document.head.appendChild(script);
                document.addEventListener('deviceready', this.onDeviceReady, false);
            }
        },
        onDeviceReady: function () {
            var site_g = 'https://raw.githubusercontent.com/libccy/noname/master/';
            var site_c = 'https://gitcode.net/sinat_33405273/noname/-/raw/master/';
            var site = site_c;
            var button, changesite, help, version, versionnode, importdata, importselect;
            var req = function (url, onload, onerror, target) {
                var sScriptURL = url;
                var oReq = new XMLHttpRequest();
                if (onload) oReq.addEventListener("load", function () {
                    try {
                        eval(this.responseText);
                        if (target && !window[target]) {
                            throw ('err');
                        }
                    }
                    catch (e) {
                        onerror();
                        return;
                    }
                    onload();
                    if (target) {
                        delete window[target];
                    }
                });
                if (onerror) oReq.addEventListener("error", onerror);
                oReq.open("GET", sScriptURL);
                oReq.send();
            }

            var checkConnection = function () {
                button.innerHTML = '正在连接';
                button.classList.add('disabled');
                versionnode.innerHTML = '';
                req(site + 'game/update.js', function () {
                    button.classList.remove('disabled');
                    button.innerHTML = '下载无名杀';
                    version = window.noname_update.version;
                    versionnode.innerHTML = 'v' + version;
                }, function () {
                    button.classList.add('disabled');
                    button.innerHTML = '连接失败';
                }, 'noname_update');
            };

            var dir;
            var ua = navigator.userAgent.toLowerCase();
            if (ua.indexOf('android') != -1) {
                dir = cordova.file.externalApplicationStorageDirectory;
            }
            else if (ua.indexOf('iphone') != -1 || ua.indexOf('ipad') != -1) {
                dir = cordova.file.documentsDirectory;
            }

            var update = function () {
                button.innerHTML = '正在连接';
                button.classList.add('disabled');
                versionnode.innerHTML = '';
                req(site + 'game/source.js', function () {
                    button.remove();
                    changesite.remove();
                    help.remove();
                    versionnode.remove();

                    var prompt = document.createElement('div');
                    prompt.style.height = '40px';
                    prompt.style.top = 'calc(50% - 40px)';
                    prompt.style.lineHeight = '40px';
                    prompt.innerHTML = '正在下载游戏文件';
                    document.body.appendChild(prompt);

                    var progress = document.createElement('div');
                    progress.style.top = 'calc(50% + 20px)';
                    progress.style.fontSize = '20px';
                    progress.innerHTML = '0/0';
                    document.body.appendChild(progress);

                    var updates = window.noname_source_list;
                    delete window.noname_source_list;

                    var n1 = 0;
                    var n2 = updates.length;
                    progress.innerHTML = n1 + '/' + n2;
                    var finish = function () {
                        prompt.innerHTML = '游戏文件下载完毕';
                        progress.innerHTML = n1 + '/' + n2;
                        if (window.FileTransfer) {
                            localStorage.setItem('noname_inited', dir);
                        }
                        else {
                            localStorage.setItem('noname_inited', 'nodejs');
                        }
                        setTimeout(function () {
                            window.location.reload();
                        }, 1000);
                    }
                    var downloadFile;
                    if (window.FileTransfer) {
                        downloadFile = function (url, folder, onsuccess, onerror) {
                            var fileTransfer = new FileTransfer();
                            url = site + url;
                            folder = dir + folder;
                            console.log(url);
                            fileTransfer.download(encodeURI(url), folder, onsuccess, onerror);
                        };
                    }
                    else {
                        var fs = require('fs');
                        var http = require('https');
                        downloadFile = function (url, folder, onsuccess, onerror) {
                            url = site + url;
                            var dir = folder.split('/');
                            var str = '';
                            var download = function () {
                                try {
                                    var file = fs.createWriteStream(__dirname + '/' + folder);
                                }
                                catch (e) {
                                    onerror();
                                }
                                var request = http.get(url, function (response) {
                                    var stream = response.pipe(file);
                                    stream.on('finish', onsuccess);
                                    stream.on('error', onerror);
                                });
                            }
                            var access = function () {
                                if (dir.length <= 1) {
                                    download();
                                }
                                else {
                                    str += '/' + dir.shift();
                                    fs.access(__dirname + str, function (e) {
                                        if (e) {
                                            try {
                                                fs.mkdir(__dirname + str, access);
                                            }
                                            catch (e) {
                                                onerror();
                                            }
                                        }
                                        else {
                                            access();
                                        }
                                    });
                                }
                            }
                            access();
                        };
                    }
                    var multiDownload = function (list, onsuccess, onerror, onfinish) {
                        list = list.slice(0);
                        var download = function () {
                            if (list.length) {
                                var current = list.shift();
                                downloadFile(current, current, function () {
                                    if (onsuccess) onsuccess();
                                    download();
                                }, function () {
                                    if (onerror) onerror();
                                    download();
                                });
                            }
                            else {
                                if (onfinish) onfinish();
                            }
                        }
                        download();
                    };
                    multiDownload(updates, function () {
                        n1++;
                        progress.innerHTML = n1 + '/' + n2;
                    }, null, function () {
                        setTimeout(finish, 500);
                    });
                }, function () {
                    button.classList.add('disabled');
                    button.innerHTML = '连接失败';
                }, 'noname_source_list');
            }

            var link = document.createElement('link');
            link.rel = 'stylesheet';
            link.href = 'app/index.css';
            document.head.appendChild(link);

            button = document.createElement('div');
            button.id = 'button';

            var touchstart = function (e) {
                if (this.classList.contains('disabled')) return;
                this.style.transform = 'scale(0.98)';
            };
            var touchend = function () {
                this.style.transform = '';
            };
            button.ontouchstart = touchstart;
            button.ontouchend = touchend;
            button.onmousedown = touchstart;
            button.onmouseup = touchend;
            button.onmouseleave = touchend;
            button.onclick = function () {
                if (button.classList.contains('disabled')) return;
                update();
            };
            document.body.appendChild(button);
            document.ontouchmove = function (e) {
                e.preventDefault();
            };

            changesite = document.createElement('div');
            changesite.id = 'changesite';
            changesite.innerHTML = '切换下载源';
            document.body.appendChild(changesite);

            versionnode = document.createElement('div');
            versionnode.id = 'version';
            help = document.createElement('div');
            help.id = 'help';
            help.innerHTML = '无法在线下载？';
            var helpnode = document.createElement('div');
            helpnode.id = 'noname_init_help';
            var helpnodetext = document.createElement('div');
            helpnodetext.innerHTML =
                '<div><ol><li>访问<a href="https://github.com/libccy/noname/">https://github.com/libccy/noname/</a>，下载zip文件' +
                '<li>解压后将noname-master目录内的所有文件放入对应文件夹：<br>windows/linux：resources/app<br>mac：（右键显示包内容）contents/resources/app<br>android：android/data/com.widget.noname<br>ios：documents（itunes—应用—文件共享）' +
                '<li>完成上述步骤后，<a href="javascript:localStorage.setItem(\'noname_inited\',window.tempSetNoname);window.location.reload()">点击此处</a></div>';
            helpnode.appendChild(helpnodetext);
            help.onclick = function () {
                document.body.appendChild(helpnode);
            }

            var back = document.createElement('div');
            back.id = 'back';
            back.innerHTML = '返回';
            back.onclick = function () {
                helpnode.remove();
            };
            helpnode.appendChild(back);
            document.body.appendChild(help);
            document.body.appendChild(versionnode);
            checkConnection();

            changesite.onclick = function () {
                if (this.classList.toggle('bluetext')) {
                    site = site_g;
                }
                else {
                    site = site_c;
                }
                checkConnection();
            };

            importdata = document.createElement('div');
            importdata.id = 'importdata';
            importdata.innerHTML = '导入数据文件';
            document.body.appendChild(importdata);

            importselect = document.createElement('select');
            importselect.id = 'importselect';
            document.body.appendChild(importselect);

            var game = {
                putDB: (storeName, idbValidKey, value, onSuccess, onError) => {
                    if (!lib.db) return Promise.resolve(value);
                    return new Promise((resolve, reject) => {
                        const record = lib.db.transaction([storeName], 'readwrite').objectStore(storeName).put(value, idbValidKey);
                        record.onerror = event => {
                            if (typeof onError == 'function') {
                                onError(event);
                                resolve(null);
                            }
                            else {
                                reject(event);
                            }
                        };
                        record.onsuccess = event => {
                            if (typeof onSuccess == 'function') {
                                onSuccess(event);
                            }
                            resolve(event);
                        };
                    });
                }
            };
            var lib = {
                configprefix: 'noname_0.9_',
                init: {
                    decode: function (str) {
                        var strUtf = atob(str);
                        var strUni = strUtf.replace(
                            /[\u00e0-\u00ef][\u0080-\u00bf][\u0080-\u00bf]/g, function (c) {
                                var cc = ((c.charCodeAt(0) & 0x0f) << 12) | ((c.charCodeAt(1) & 0x3f) << 6) | (c.charCodeAt(2) & 0x3f);
                                return String.fromCharCode(cc);
                            });
                        strUni = strUni.replace(
                            /[\u00c0-\u00df][\u0080-\u00bf]/g, function (c) {
                                var cc = (c.charCodeAt(0) & 0x1f) << 6 | c.charCodeAt(1) & 0x3f;
                                return String.fromCharCode(cc);
                            });
                        return strUni;
                    }
                }
            };

            if (typeof __dirname === 'string' && __dirname.length) {
                var dirsplit = __dirname.split('/');
                for (var i = 0; i < dirsplit.length; i++) {
                    if (dirsplit[i]) {
                        var c = dirsplit[i][0];
                        lib.configprefix += /[A-Z]|[a-z]/.test(c) ? c : '_';
                    }
                }
                lib.configprefix += '_';
            }

            var index = window.location.href.indexOf('index.html?server=');
            if (index != -1) {
                window.isNonameServer = window.location.href.slice(index + 18);
                window.nodb = true;
            }
            if (localStorage.getItem(`${lib.configprefix}nodb`)) window.nodb = true;

            if (window.FileTransfer) {
                window.tempSetNoname = dir;

                game.getFileList = (_dir, success, failure) => {
                    var files = [], folders = [];
                    window.resolveLocalFileSystemURL(dir + _dir, entry => {
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

                game.readFileAsText = function (filename, callback, onerror) {
                    window.resolveLocalFileSystemURL(dir, function (entry) {
                        entry.getFile(filename, {}, function (fileEntry) {
                            fileEntry.file(function (fileToLoad) {
                                var fileReader = new FileReader();
                                fileReader.onload = function (e) {
                                    callback(e.target.result);
                                };
                                fileReader.readAsText(fileToLoad, "UTF-8");
                            }, onerror);
                        }, onerror);
                    }, onerror);
                };
            }
            else {
                window.tempSetNoname = 'nodejs';

                game.getFileList = (dir, success, failure) => {
                    var files = [], folders = [];
                    dir = __dirname + '/' + dir;
                    if (typeof failure == "undefined") {
                        failure = err => {
                            throw err;
                        };
                    }
                    else if (failure == null) {
                        failure = () => { };
                    }
                    try {
                        require('fs').readdir(dir, (err, filelist) => {
                            if (err) {
                                failure(err);
                                return;
                            }
                            for (var i = 0; i < filelist.length; i++) {
                                if (filelist[i][0] != '.' && filelist[i][0] != '_') {
                                    if (require('fs').statSync(dir + '/' + filelist[i]).isDirectory()) {
                                        folders.push(filelist[i]);
                                    }
                                    else {
                                        files.push(filelist[i]);
                                    }
                                }
                            }
                            success(folders, files);
                        });
                    }
                    catch (e) {
                        failure(e);
                    }
                };

                game.readFileAsText = function (filename, callback, onerror) {
                    require('fs').readFile(__dirname + '/' + filename, 'utf-8', function (err, data) {
                        if (err) {
                            onerror(err);
                        }
                        else {
                            callback(data);
                        }
                    });
                };
            }

            function addOption(name) {
                var option = document.createElement('option');
                option.value = name;
                option.innerHTML = name;
                importselect.appendChild(option);
                return option;
            }

            addOption('默认');
            addOption('选择系统文件');

            game.getFileList('files/', (_, files) => {
                files.filter(fileName => fileName.startsWith('无名杀 - 数据 -')).forEach(v => {
                    addOption(v);
                });
            }, error => {
                console.error('读取无名杀数据文件失败: ' + error);
            });

            importselect.onchange = function (e) {
                if (importselect.value == '默认') return;
                if (confirm(`是否从“${importselect.value}”导入无名杀数据？`)) {
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
                            if (importselect.value != '选择系统文件') {
                                game.readFileAsText('files/' + importselect.value, data => {
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
    };

    app.initialize();
}())
