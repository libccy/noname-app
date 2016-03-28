'use strict';
(function(){
    if(localStorage.getItem('noname_inited')) return;
    var app = {
        initialize: function() {
            this.bindEvents();
        },
        bindEvents: function() {
            document.addEventListener('deviceready', this.onDeviceReady, false);
        },
        onDeviceReady: function() {
            var link=document.createElement('link');
            link.rel='stylesheet';
            link.href='index.css';
            document.head.appendChild(link);

            var site=localStorage.getItem('noname_download_source')||'http://isha.applinzi.com/';

            var button=document.createElement('div');
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
            button.onclick=function(){
                update();
            };
            document.body.appendChild(button);
            document.ontouchmove=function(e){
                e.preventDefault();
            };

            var changesite=document.createElement('div');
            changesite.style.top='calc(50% + 30px)';
            changesite.style.fontSize='20px';
            changesite.innerHTML='选择下载源';
            changesite.ontouchstart=touchstart;
            changesite.ontouchend=touchend;
            document.body.appendChild(changesite);
            changesite.onclick=function(){
                site=prompt('选择下载源',site)||site;
            }

            var dir;
            var ua=navigator.userAgent.toLowerCase();
            if(ua.indexOf('android')!=-1){
                dir=externalApplicationStorageDirectory;
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
    				var n1=0;
    				var n2=updates.length;
    				var n=n2;
    				progress.innerHTML=n1+'/'+n2;
    				var finish=function(){
                        prompt.innerHTML='游戏文件下载完毕';
    					progress.innerHTML=n1+'/'+n2;
                        localStorage.setItem('noname_inited',dir);
    					setTimeout(function(){
                            window.location.reload();
                        },1000);
    				}
                    var download=function(url,folder,onsuccess,onerror){
    					var fileTransfer = new FileTransfer();
    					url=site+url;
    					folder=dir+folder;
    					fileTransfer.download(encodeURI(url),folder,onsuccess,onerror);
    				};
    				for(var i=0;i<updates.length;i++){
    					download(updates[i],updates[i],function(){
    						n--;
    						n1++;
    						progress.innerHTML=n1+'/'+n2;
    						if(n==0){
    							setTimeout(finish,500);
    						}
    					},function(e){
    						n--;
    						progress.innerHTML=n1+'/'+n2;
    						if(n==0){
    							setTimeout(finish,500);
    						}
    					});
    				}
                };
                script.onerror=function(){
                    alert('连接失败');
                    window.location.reload();
                };
                document.head.appendChild(script);
            }
        }
    };

    app.initialize();
}())
