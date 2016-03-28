'use strict';
(function(){
    if(localStorage.getItem('noname_inited')) return;
    var app = {
        initialize: function() {
            this.bindEvents();
        },
        bindEvents: function() {
            if(window.require&&window.__dirname){
                this.onDeviceReady();
            }
            else{
                var script=document.createElement('script');
                script.src='cordova.js';
                document.head.appendChild(script);
                document.addEventListener('deviceready', this.onDeviceReady, false);
            }
        },
        onDeviceReady: function() {
            var site=localStorage.getItem('noname_download_source')||'http://isha.applinzi.com/';
            var button,changesite;

            var dir;
            var ua=navigator.userAgent.toLowerCase();
            if(ua.indexOf('android')!=-1){
                dir=cordova.file.externalApplicationStorageDirectory;
            }
            else if(ua.indexOf('iphone')!=-1||ua.indexOf('ipad')!=-1){
                dir=cordova.file.documentsDirectory;
            }

            var update=function(){
                button.remove();
                changesite.remove();
                if(site.indexOf('http://')!=0){
                    site='http://'+site;
                }
                if(site[site.length-1]!='/'){
                    site+='/';
                }

                var prompt=document.createElement('div');
                prompt.style.height='40px';
                prompt.style.top='calc(50% - 40px)';
                prompt.style.lineHeight='40px';
                prompt.innerHTML='正在下载游戏文件';
                document.body.appendChild(prompt);

                var progress=document.createElement('div');
                progress.style.top='calc(50% + 20px)';
                progress.style.fontSize='20px';
                progress.innerHTML='0/0';
                document.body.appendChild(progress);

                var script=document.createElement('script');
                script.src=site+'game/source.js';
                script.onload=function(){
                    localStorage.setItem('noname_download_source',site);
                    var updates=window.noname_source_list;
                    delete window.noname_source_list;

    				var n1=0;
    				var n2=updates.length;
    				progress.innerHTML=n1+'/'+n2;
    				var finish=function(){
                        prompt.innerHTML='游戏文件下载完毕';
    					progress.innerHTML=n1+'/'+n2;
                        if(window.FileTransfer){
                            localStorage.setItem('noname_inited',dir);
                        }
                        else{
                            localStorage.setItem('noname_inited','nodejs');
                        }
    					setTimeout(function(){
                            window.location.reload();
                        },1000);
    				}
                    var downloadFile;
                    if(window.FileTransfer){
                        downloadFile=function(url,folder,onsuccess,onerror){
        					var fileTransfer = new FileTransfer();
        					url=site+url;
        					folder=dir+folder;
        					fileTransfer.download(encodeURI(url),folder,onsuccess,onerror);
        				};
                    }
                    else{
                        var fs=require('fs');
                        var http=require('http');
                        downloadFile=function(url,folder,onsuccess,onerror){
                            url=site+url;
                            var dir=folder.split('/');
                            var str='';
                            var download=function(){
                                try{
                                    var file = fs.createWriteStream(__dirname+'/'+folder);
                                }
                                catch(e){
                                    onerror();
                                }
                                var request = http.get(url, function(response) {
                                    var stream=response.pipe(file);
                                    stream.on('finish',onsuccess);
                                    stream.on('error',onerror);
                                });
                            }
                            var access=function(){
                                if(dir.length<=1){
                                    download();
                                }
                                else{
                                    str+='/'+dir.shift();
                                    fs.access(__dirname+str,function(e){
                                        if(e){
                                            try{
                                                fs.mkdir(__dirname+str,access);
                                            }
                                            catch(e){
                                                onerror();
                                            }
                                        }
                                        else{
                                            access();
                                        }
                                    });
                                }
                            }
                            access();
                        };
                    }
                    var multiDownload=function(list,onsuccess,onerror,onfinish){
                        list=list.slice(0);
                        var download=function(){
                            if(list.length){
                                var current=list.shift();
                                downloadFile(current,current,function(){
                                    if(onsuccess) onsuccess();
                                    download();
                                },function(){
                                    if(onerror) onerror();
                                    download();
                                });
                            }
                            else{
                                if(onfinish) onfinish();
                            }
                        }
                        download();
                    };
                    multiDownload(updates,function(){
                        n1++;
                        progress.innerHTML=n1+'/'+n2;
                    },null,function(){
                        setTimeout(finish,500);
                    });
                };
                script.onerror=function(){
                    alert('连接失败');
                    window.location.reload();
                };
                document.head.appendChild(script);
            }

            var link=document.createElement('link');
            link.rel='stylesheet';
            link.href='app/index.css';
            document.head.appendChild(link);

            button=document.createElement('div');
            button.id='button';
            button.innerHTML='下载无名杀';

            var touchstart=function(e){
                this.style.transform='scale(0.98)';
            };
            var touchend=function(){
                this.style.transform='';
            };
            button.ontouchstart=touchstart;
            button.ontouchend=touchend;
            button.onmousedown=touchstart;
            button.onmouseup=touchend;
            button.onmouseleave=touchend;
            button.onclick=function(){
                update();
            };
            document.body.appendChild(button);
            document.ontouchmove=function(e){
                e.preventDefault();
            };

            changesite=document.createElement('div');
            changesite.style.top='calc(50% + 30px)';
            changesite.style.fontSize='20px';
            changesite.innerHTML='选择下载源';
            changesite.ontouchstart=touchstart;
            changesite.ontouchend=touchend;
            changesite.onmousedown=touchstart;
            changesite.onmouseup=touchend;
            changesite.onmouseleave=touchend;
            document.body.appendChild(changesite);
            changesite.onclick=function(){
                site=prompt('选择下载源',site)||site;
            }
        }
    };

    app.initialize();
}())
