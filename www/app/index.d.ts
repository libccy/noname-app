type K = keyof GlobalEventHandlers;

interface EvtHandlers {
	[key: K]: GlobalEventHandlers[K];
}

type CKey = keyof CSSStyleDeclaration;

interface CssHandlers {
	[key: string]: CSSStyleDeclaration[CKey];
}

declare interface options {
	class?: string[];
	id?: string;
	innerHTML?: string;
	innerText?: string;
	parentNode?: HTMLElement | HTMLDivElement;
	listen?: EvtHandlers;
	style?: CssHandlers;
}

declare interface Window {
	JSZip3: any;
}

declare interface progress extends HTMLDivElement {
    /** 获取标题 */
    getTitle: () => string;
    /** 更改标题 */
    setTitle: (title: string) => void;
    /** 获取显示的文件名 */
    getFileName: () => string;
    /** 更改显示的文件名 */
    setFileName: (title: string) => void;
    /** 获取进度*/
    getProgressValue: () => number;
    /** 更改进度*/
    setProgressValue: (value: number) => void;
    /** 获取下载文件总数 */
    getProgressMax: () => number;
    /** 修改下载文件总数 */
    setProgressMax: (max: number) => void;
    /** 通过数组自动解析文件名 */
    autoSetFileNameFromArray: (fileNameList: string[]) => void;
}