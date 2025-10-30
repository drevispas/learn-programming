import { useState } from 'react';
import { useToast } from '@toss-design-system/react-native';
import { IAP } from '@apps-in-toss/framework';

export function usePurchase() {
  const toast = useToast();
  const [loading, setLoading] = useState<boolean>(false);

  const purchaseProduct = async (productId: string) => {
    if (loading) {
      return;
    }

    setLoading(true);
    try {
      await IAP.createOneTimePurchaseOrder({ productId });
      toast.open('구매 했어요.');
    } catch (error) {
      toast.open('구매를 실패했어요.');
      console.error(error);
    } finally {
      setLoading(false);
    }
  };

  return { purchaseProduct, loading };
}
