export interface User {
  id: number;
  username: string;
  email: string;
  roles: string[];
}

export enum ERole {
  ROLE_USER = 'ROLE_USER',
  ROLE_MODERATOR = 'ROLE_MODERATOR',
  ROLE_ADMIN = 'ROLE_ADMIN',
}
