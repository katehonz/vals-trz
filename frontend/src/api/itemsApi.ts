import type { Item } from '../types/Item';

const BASE_URL = '/api/items';

export async function fetchItems(): Promise<Item[]> {
  const response = await fetch(BASE_URL);
  if (!response.ok) {
    throw new Error(`Failed to fetch items: ${response.statusText}`);
  }
  return response.json();
}

export async function createItem(
  item: Omit<Item, 'id' | 'arangoId'>
): Promise<Item> {
  const response = await fetch(BASE_URL, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(item),
  });
  if (!response.ok) {
    throw new Error(`Failed to create item: ${response.statusText}`);
  }
  return response.json();
}

export async function deleteItem(id: string): Promise<void> {
  const response = await fetch(`${BASE_URL}/${id}`, {
    method: 'DELETE',
  });
  if (!response.ok) {
    throw new Error(`Failed to delete item: ${response.statusText}`);
  }
}
