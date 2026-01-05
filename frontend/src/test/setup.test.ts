import { describe, it, expect } from 'vitest';

describe('Frontend Setup', () => {
    it('should be configured correctly', () => {
        expect(true).toBe(true);
    });

    it('should have jsdom environment', () => {
        const element = document.createElement('div');
        expect(element).not.toBeNull();
    });
});
