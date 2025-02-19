/**
 * @author yunfanzhang@kuainiugroup.com
 * @date 2024/9/19
*/
export interface NormalVO {
   name: string
   address: string
   number: number
}

/**
 * @author yunfanzhang@kuainiugroup.com
 * @date 2024/9/18
*/
export interface InnerClass {
   id: number
   info: string
}

/**
 * @author yunfanzhang@kuainiugroup.com
 * @date 2024/9/18
*/
export interface StaticClassTestPojo {
   name: string
   age: number
   innerClass: InnerClass
}
