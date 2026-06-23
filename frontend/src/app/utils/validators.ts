/**
 * Validate email address
 */
export function isValidEmail(email: string): boolean {
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  return emailRegex.test(email);
}

/**
 * Validate phone number (French format)
 */
export function isValidPhoneNumber(phone: string): boolean {
  const phoneRegex = /^(?:(?:\+|00)33|0)\s*[1-9](?:[\s.-]*\d{2}){4}$/;
  return phoneRegex.test(phone.replace(/\s/g, ''));
}

/**
 * Validate URL
 */
export function isValidURL(url: string): boolean {
  try {
    new URL(url);
    return true;
  } catch {
    return false;
  }
}

/**
 * Validate French postal code
 */
export function isValidPostalCode(code: string): boolean {
  return /^\d{5}$/.test(code);
}

/**
 * Validate French SIREN number
 */
export function isValidSIREN(siren: string): boolean {
  const cleaned = siren.replace(/\s/g, '');
  if (!/^\d{9}$/.test(cleaned)) return false;
  
  let sum = 0;
  for (let i = 0; i < 9; i++) {
    let digit = parseInt(cleaned[i]);
    if (i % 2 === 1) {
      digit *= 2;
      if (digit > 9) digit -= 9;
    }
    sum += digit;
  }
  
  return sum % 10 === 0;
}

/**
 * Validate French SIRET number
 */
export function isValidSIRET(siret: string): boolean {
  const cleaned = siret.replace(/\s/g, '');
  if (!/^\d{14}$/.test(cleaned)) return false;
  
  const siren = cleaned.substring(0, 9);
  const siretNumber = cleaned.substring(9, 14);
  
  return isValidSIREN(siren);
}

/**
 * Validate IBAN
 */
export function isValidIBAN(iban: string): boolean {
  const cleaned = iban.replace(/\s/g, '').toUpperCase();
  if (!/^[A-Z]{2}\d{2}[A-Z0-9]{11,30}$/.test(cleaned)) return false;
  
  const rearranged = cleaned.substring(4) + cleaned.substring(0, 4);
  const numeric = rearranged.split('').map(char => {
    const code = char.charCodeAt(0);
    return code >= 65 && code <= 90 ? code - 55 : char;
  }).join('');
  
  let remainder = BigInt(numeric) % BigInt(97);
  return remainder === BigInt(1);
}

/**
 * Validate credit card number (Luhn algorithm)
 */
export function isValidCreditCard(number: string): boolean {
  const cleaned = number.replace(/\s/g, '');
  if (!/^\d{13,19}$/.test(cleaned)) return false;
  
  let sum = 0;
  let isEven = false;
  
  for (let i = cleaned.length - 1; i >= 0; i--) {
    let digit = parseInt(cleaned[i]);
    
    if (isEven) {
      digit *= 2;
      if (digit > 9) digit -= 9;
    }
    
    sum += digit;
    isEven = !isEven;
  }
  
  return sum % 10 === 0;
}

/**
 * Validate password strength
 */
export function getPasswordStrength(password: string): {
  score: number;
  feedback: string;
} {
  let score = 0;
  const feedback: string[] = [];
  
  if (password.length >= 8) {
    score += 1;
  } else {
    feedback.push('Au moins 8 caractères');
  }
  
  if (/[a-z]/.test(password)) {
    score += 1;
  } else {
    feedback.push('Une lettre minuscule');
  }
  
  if (/[A-Z]/.test(password)) {
    score += 1;
  } else {
    feedback.push('Une lettre majuscule');
  }
  
  if (/\d/.test(password)) {
    score += 1;
  } else {
    feedback.push('Un chiffre');
  }
  
  if (/[^a-zA-Z0-9]/.test(password)) {
    score += 1;
  } else {
    feedback.push('Un caractère spécial');
  }
  
  return {
    score,
    feedback: feedback.length > 0 ? feedback.join(', ') : 'Mot de passe fort'
  };
}

/**
 * Validate text length
 */
export function isValidLength(text: string, min: number, max: number): boolean {
  return text.length >= min && text.length <= max;
}

/**
 * Validate number range
 */
export function isInRange(value: number, min: number, max: number): boolean {
  return value >= min && value <= max;
}

/**
 * Validate required field
 */
export function isRequired(value: any): boolean {
  if (value === null || value === undefined) return false;
  if (typeof value === 'string') return value.trim().length > 0;
  if (Array.isArray(value)) return value.length > 0;
  return true;
}

/**
 * Validate date is in the future
 */
export function isFutureDate(date: Date): boolean {
  return date > new Date();
}

/**
 * Validate date is in the past
 */
export function isPastDate(date: Date): boolean {
  return date < new Date();
}

/**
 * Validate age (minimum and maximum)
 */
export function isValidAge(birthDate: Date, minAge: number = 0, maxAge: number = 150): boolean {
  const today = new Date();
  const age = today.getFullYear() - birthDate.getFullYear();
  const monthDiff = today.getMonth() - birthDate.getMonth();
  
  const actualAge = monthDiff < 0 || (monthDiff === 0 && today.getDate() < birthDate.getDate())
    ? age - 1
    : age;
  
  return actualAge >= minAge && actualAge <= maxAge;
}

/**
 * Validate percentage (0-100)
 */
export function isValidPercentage(value: number): boolean {
  return value >= 0 && value <= 100;
}

/**
 * Validate hexadecimal color
 */
export function isValidHexColor(color: string): boolean {
  return /^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$/.test(color);
}

/**
 * Validate RGB color
 */
export function isValidRGBColor(color: string): boolean {
  return /^rgb\(\s*\d+\s*,\s*\d+\s*,\s*\d+\s*\)$/.test(color);
}

/**
 * Validate French social security number (NIR)
 */
export function isValidNIR(nir: string): boolean {
  const cleaned = nir.replace(/\s/g, '');
  if (!/^[12]\d{13}$/.test(cleaned)) return false;
  
  // Simplified validation - full validation requires more complex logic
  return cleaned.length === 15;
}

/**
 * Validate VAT number (TVA)
 */
export function isValidVATNumber(vat: string, countryCode: string = 'FR'): boolean {
  const cleaned = vat.replace(/\s/g, '').toUpperCase();
  
  if (!cleaned.startsWith(countryCode)) return false;
  
  const number = cleaned.substring(countryCode.length);
  
  // Simplified validation - full validation depends on country
  return number.length >= 2 && /^\d+$/.test(number);
}

/**
 * Validate username (alphanumeric with underscores and hyphens)
 */
export function isValidUsername(username: string): boolean {
  return /^[a-zA-Z0-9_-]{3,20}$/.test(username);
}

/**
 * Validate slug (URL-friendly)
 */
export function isValidSlug(slug: string): boolean {
  return /^[a-z0-9]+(?:-[a-z0-9]+)*$/.test(slug);
}

/**
 * Validate JSON string
 */
export function isValidJSON(json: string): boolean {
  try {
    JSON.parse(json);
    return true;
  } catch {
    return false;
  }
}

/**
 * Validate date format (DD/MM/YYYY)
 */
export function isValidDateFormat(date: string): boolean {
  return /^\d{2}\/\d{2}\/\d{4}$/.test(date);
}

/**
 * Validate time format (HH:MM)
 */
export function isValidTimeFormat(time: string): boolean {
  return /^([01]?[0-9]|2[0-3]):[0-5][0-9]$/.test(time);
}

/**
 * Validate file extension
 */
export function isValidFileExtension(filename: string, allowedExtensions: string[]): boolean {
  const extension = filename.split('.').pop()?.toLowerCase();
  return extension ? allowedExtensions.includes(extension) : false;
}

/**
 * Validate file size
 */
export function isValidFileSize(size: number, maxSizeInMB: number): boolean {
  const maxSizeInBytes = maxSizeInMB * 1024 * 1024;
  return size <= maxSizeInBytes;
}

/**
 * Validate that value is not empty object
 */
export function isNotEmptyObject(obj: any): boolean {
  return obj && Object.keys(obj).length > 0;
}

/**
 * Validate that value is not empty array
 */
export function isNotEmptyArray(arr: any[]): boolean {
  return arr && arr.length > 0;
}

/**
 * Custom validator with error message
 */
export interface ValidationResult {
  valid: boolean;
  error?: string;
}

export function validate(
  value: any,
  validator: (value: any) => boolean,
  errorMessage: string
): ValidationResult {
  const valid = validator(value);
  return {
    valid,
    error: valid ? undefined : errorMessage
  };
}

/**
 * Chain multiple validators
 */
export function validateChain(
  value: any,
  validators: Array<(value: any) => ValidationResult>
): ValidationResult {
  for (const validator of validators) {
    const result = validator(value);
    if (!result.valid) {
      return result;
    }
  }
  return { valid: true };
}
