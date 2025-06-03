/**
 * @author yunfanzhang@kuainiugroup.com
 * @date 2024/9/19
*/
export interface NormalVO {
   /**
    * 姓名
   */
   name: string
   /**
    * 地址
   */
   address: string
   /**
    * 号码
   */
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
