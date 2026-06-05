import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import { environment } from '../../environments/environment';

export interface User {
  idUser?: string;
  username: string;
  email: string;
  password?: string;
  phone?: number;
  dateCreation?: string;
  departement?: string;
}

export interface LoginResponse {
  token: string;
  utilisateur: User;
}

export interface RegisterRequest {
  username: string;
  password: string;
  email: string;
  phone: number;
  departement: string;
}

@Injectable({
  providedIn: 'root'
})
export class GestionUserService {
  private readonly apiUrl = `${environment.apiUser}`;
  private readonly tokenStorageKey = 'token';
  private readonly currentUserStorageKey = 'currentUser';

  private readonly currentUserSubject = new BehaviorSubject<User | null>(this.readStoredUser());
  readonly currentUser$ = this.currentUserSubject.asObservable();


  constructor(private http: HttpClient) {}


  // Authentification
  login(credentials: { username: string; password: string }): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.apiUrl}/auth/login`, credentials).pipe(
      tap((response) => {
        if (response?.token && response?.utilisateur) {
          this.setSession(response.token, response.utilisateur);
        }
      })
    );
  }

  refreshToken(refreshToken: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/auth/refresh`, { refreshToken });
  }

  register(request: RegisterRequest): Observable<User> {
    return this.http.post<User>(`${this.apiUrl}/auth/register`, request);
  }

  logout(): void {
    localStorage.removeItem(this.tokenStorageKey);
    localStorage.removeItem(this.currentUserStorageKey);
    localStorage.removeItem('refreshToken');
    this.currentUserSubject.next(null);
  }

  get currentUserValue(): User | null {
    return this.currentUserSubject.value;
  }

  getToken(): string | null {
    return localStorage.getItem(this.tokenStorageKey);
  }

  private setSession(token: string, user: User): void {
    localStorage.setItem(this.tokenStorageKey, token);
    localStorage.setItem(this.currentUserStorageKey, JSON.stringify(user));
    this.currentUserSubject.next(user);
  }

  private readStoredUser(): User | null {
    const raw = localStorage.getItem(this.currentUserStorageKey);
    if (!raw) {
      return null;
    }

    try {
      return JSON.parse(raw) as User;
    } catch {
      return null;
    }
  }

  // Gestion des utilisateurs
  getAllUsers(): Observable<User[]> {
    return this.http.get<User[]>(`${this.apiUrl}/users`);
  }

  getUserById(idUser: string): Observable<User> {
    return this.http.get<User>(`${this.apiUrl}/users/${idUser}`);
  }

  deleteUser(idUser: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/users/${idUser}`);
  }

}
