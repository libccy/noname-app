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
            var site='https://raw.githubusercontent.com/libccy/noname/master/';
            var button,changesite,help;

            var script=document.createElement('script');
            script.src='https://rawgit.com/libccy/noname/master/game/update.js';
            script.onload=function(){
                if(window.noname_update){
                    site=site.replace(/master/,'v'+window.noname_update.version);
                    button.classList.remove('disabled');
					delete window.noname_update;
				}
            }
            script.onerror=function(){
                document.body.appendChild(helpnode);
            }
            document.head.appendChild(script);

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
                help.remove();

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
                script.src=site.replace(/raw\.githubusercontent/,'rawgit')+'game/source.js';
                script.onload=function(){
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
                        var http=require('https');
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
            button.classList.add('disabled');

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
                if(button.classList.contains('disabled')) return;
                update();
            };
            document.body.appendChild(button);
            document.ontouchmove=function(e){
                e.preventDefault();
            };

            changesite=document.createElement('div');
            changesite.style.top='calc(50% + 30px)';
            changesite.style.width='220px';
            changesite.style.left='calc(50% - 110px)';
            changesite.style.fontSize='20px';
            changesite.innerHTML='选择下载源';
            // document.body.appendChild(changesite);

            help=document.createElement('div');
            help.style.bottom='20px';
            help.style.width='180px';
            help.style.left='auto';
            help.style.right='20px';
            help.style.textAlign='right';
            help.style.fontSize='20px';
            help.innerHTML='无法在线下载？';
            var helpnode=document.createElement('div');
            helpnode.id='noname_init_help';
            var helpnodetext=document.createElement('div');
            helpnodetext.innerHTML=
            '<div><ol><li>访问<a href="https://github.com/libccy/noname/releases/latest">https://github.com/libccy/noname/releases/latest</a>，下载zip文件'+
            '<li>解压后将noname-master目录内的所有文件放入对应文件夹：<br>windows/linux：resources/app<br>mac：（右键显示包内容）contents/resources/app<br>android：android/data/com.widget.noname<br>ios：documents（itunes—应用—文件共享）'+
            '<li>完成上述步骤后，<a href="javascript:localStorage.setItem(\'noname_inited\',window.tempSetNoname);window.location.reload()">点击此处</a></div>';
            helpnode.appendChild(helpnodetext);
            help.onclick=function(){
                document.body.appendChild(helpnode);
            }

            var back=document.createElement('div');
            back.classList.add('back');
            back.style.width='60px';
            back.style.bottom='20px';
            back.style.left='auto';
            back.style.right='20px';
            back.style.textAlign='right';
            back.style.fontSize='20px';
            back.innerHTML='返回';
            back.onclick=function(){
                helpnode.remove();
            };
            helpnode.appendChild(back);
            document.body.appendChild(help);
            if(window.FileTransfer){
                window.tempSetNoname=dir;
            }
            else{
                window.tempSetNoname='nodejs';
            }


            if(window.cordova){
                changesite.onclick=function(){
                    site=prompt('选择下载源',site)||site;
                }
            }
            else{
                changesite.style.outline='none';
                changesite.onclick=function(){
                    if(this.contentEditable!=true){
                        this.contentEditable=true;
                        changesite.style.webkitUserSelect='text';
                        this.innerHTML=site;
                        this.focus();
                    }
                }
                changesite.onblur=function(){
                    if(this.innerHTML!='选择下载源'){
                        site=this.innerHTML;
                        this.innerHTML='选择下载源';
                    }
                    this.contentEditable=false;
                    changesite.style.webkitUserSelect='none';
                }
                changesite.onkeydown=function(e){
                    if(e.keyCode==13){
                        e.preventDefault();
                        this.blur();
                    }
                }
            }
        }
    };

    app.initialize();
}())
