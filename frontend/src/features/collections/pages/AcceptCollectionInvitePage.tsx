import React, { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { Box, Button, Typography } from '@mui/material';
import { validateInvite, acceptInvite } from '../api/collectionInvitesApi';

export default function AcceptCollectionInvitePage(){
  const { token } = useParams<{ token: string }>();
  const [info, setInfo] = useState<any>(null);
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  useEffect(()=>{
    if (!token) return;
    (async ()=>{
      try{
        const data = await validateInvite(token);
        setInfo(data);
      }catch(err:any){
        setInfo({ error: err.message || 'Invalid invite' });
      }
    })();
  }, [token]);

  if (!token) return <Typography>Invalid invite token</Typography>;
  if (!info) return <Typography>Loading...</Typography>;
  if (info.error) return <Typography color="error">{info.error}</Typography>;

  const doAccept = async () => {
    setLoading(true);
    try{
      const res = await acceptInvite(token);
      const collectionId = res.collectionId || info.collectionId;
      navigate(`/collections/${collectionId}`);
    }catch(err:any){
      alert(err.message || 'Failed to accept invite');
    }finally{ setLoading(false); }
  };

  return (
    <Box sx={{ p:4 }}>
      <Typography variant="h5">You've been invited</Typography>
      <Typography>Role: {info.role}</Typography>
      <Box sx={{ mt:2 }}>
        <Button variant="contained" onClick={doAccept} disabled={loading}>Accept invite</Button>
      </Box>
    </Box>
  );
}
