declare namespace Deno {
  export interface Env {
    get(key: string): string | undefined;
    set(key: string, value: string): void;
    delete(key: string): void;
    toObject(): Record<string, string>;
  }
  export const env: Env;
  export function serve(handler: (req: Request) => Response | Promise<Response>): void;
}

declare module "@supabase/supabase-js" {
  export function createClient(supabaseUrl: string, supabaseKey: string, options?: any): any;
}
