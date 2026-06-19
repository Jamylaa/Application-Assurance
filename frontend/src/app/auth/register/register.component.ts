import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder,FormGroup,ReactiveFormsModule,Validators} from '@angular/forms';
import { Router,RouterModule} from '@angular/router';
import { InputTextModule} from 'primeng/inputtext';
import {  PasswordModule} from 'primeng/password';
import { ButtonModule} from 'primeng/button';
import {  MessageModule} from 'primeng/message';
import {GestionUserService,  RegisterRequest} from '../../services/gestion-user.service';


@Component({
  selector: 'app-register',
  standalone: true,
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.css'],

  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterModule,
    InputTextModule,
    PasswordModule,
    ButtonModule,
    MessageModule
  ]
})

export class RegisterComponent {

  registerForm: FormGroup;

  loading = false;

  error = '';

  success = '';


  constructor(
    private readonly fb: FormBuilder,
    private readonly userService: GestionUserService,
    private readonly router: Router
  ) {

    this.registerForm = this.fb.group({

      username: [
        '',
        [
          Validators.required,
          Validators.minLength(3)
        ]
      ],

      email: [
        '',
        [
          Validators.required,
          Validators.email
        ]
      ],

      password: [
        '',
        [
          Validators.required,
          Validators.minLength(7)
        ]
      ],

      phone: [
        '',
        Validators.required
      ],

      departement: [
        'IT',
        Validators.required
      ]
    });
  }


  onSubmit(): void {

    if (this.registerForm.invalid) {
      this.registerForm.markAllAsTouched();
      return;
    }

    this.loading = true;

    this.error = '';

    this.success = '';

    const payload: RegisterRequest = {

      username: this.registerForm.value.username,

      email: this.registerForm.value.email,

      password: this.registerForm.value.password,

      phone: Number(this.registerForm.value.phone),

      departement: this.registerForm.value.departement
    };

    this.userService.register(payload).subscribe({

      next: () => {

        this.loading = false;

        this.success =
          'Compte créé avec succès';

        setTimeout(() => {

          this.router.navigate([
            '/auth/login'
          ]);

        }, 1500);
      },

      error: (err) => {

        this.loading = false;

        this.error =
          err?.error?.message ||
          'Erreur lors de la création du compte';
      }
    });
  }
}