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
            var dir;
            var ua=navigator.userAgent.toLowerCase();
            if(ua.indexOf('android')!=-1){
                dir=externalApplicationStorageDirectory;
            }
            else if(ua.indexOf('iphone')!=-1||ua.indexOf('ipad')!=-1){
                dir=cordova.file.documentsDirectory;
            }

            var link=document.createElement('link');
            link.rel='stylesheet';
            link.href='index.css';
            document.head.appendChild(link);

            var prompt=document.createElement('div');
            prompt.style.height='40px';
            prompt.style.top='calc(50% - 50px)';
            prompt.style.lineHeight='40px';
            prompt.innerHTML='正在下载游戏文件';
            document.body.appendChild(prompt);

            var progress=document.createElement('div');
            progress.style.top='calc(50% + 10px)';
            progress.style.fontSize='20px';
            progress.innerHTML='0/0';
            document.body.appendChild(progress);

            var script=document.createElement('script');
            script.src='http://isha.applinzi.com/game/source.js';
            script.onload=function(){
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
					url='http://isha.applinzi.com/'+url;
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
            document.head.appendChild(script);
        }
    };

    app.initialize();
}())
